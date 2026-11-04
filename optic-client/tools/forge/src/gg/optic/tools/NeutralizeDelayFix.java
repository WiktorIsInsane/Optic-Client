package gg.optic.tools;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Turns off the old Lunar path's "Delay Fix" attack scheduler in lunar-localpatches.jar.
 * Its hooks re-fired clicks on a 50 ms grid and dropped attack packets sent close
 * together. After this, every hook returns at once and every attack packet is allowed,
 * so clicks and attacks are exactly vanilla.
 */
public final class NeutralizeDelayFix {
	private static final String HOOKS = "com/moonsworth/lunar/client/mod/combat/delayfix/DelayFixHooks.class";

	public static void main(String[] args) throws Exception {
		for (String arg : args) patch(Paths.get(arg));
	}

	private static void patch(Path jar) throws Exception {
		Path out = jar.resolveSibling(jar.getFileName() + ".tmp");
		boolean changed = false;
		try (ZipFile zip = new ZipFile(jar.toFile()); ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(out))) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				byte[] data = read(zip.getInputStream(entry));
				if (HOOKS.equals(entry.getName())) {
					data = rewrite(data);
					changed = true;
				}
				ZipEntry copy = new ZipEntry(entry.getName());
				copy.setTime(entry.getTime());
				zos.putNextEntry(copy);
				zos.write(data);
				zos.closeEntry();
			}
		}
		if (!changed) {
			Files.delete(out);
			System.out.println("no DelayFixHooks in " + jar);
			return;
		}
		Files.move(out, jar, StandardCopyOption.REPLACE_EXISTING);
		System.out.println("neutralized " + jar);
	}

	private static byte[] rewrite(byte[] data) {
		ClassNode node = new ClassNode();
		new ClassReader(data).accept(node, 0);
		for (MethodNode method : node.methods) {
			boolean hook = method.name.equals("onRunTickStart") || method.name.equals("onFrameBegin") || method.name.equals("onBeforeClick");
			boolean gate = method.name.equals("onBeforeAttackPacket");
			if (!hook && !gate) continue;
			InsnList body = new InsnList();
			if (gate) {
				body.add(new InsnNode(Opcodes.ICONST_1));
				body.add(new InsnNode(Opcodes.IRETURN));
			} else {
				body.add(new InsnNode(Opcodes.RETURN));
			}
			method.instructions = body;
			method.tryCatchBlocks.clear();
			method.localVariables = null;
			method.maxStack = gate ? 1 : 0;
		}
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		return writer.toByteArray();
	}

	private static byte[] read(InputStream in) throws Exception {
		try (InputStream input = in) {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] chunk = new byte[8192];
			int n;
			while ((n = input.read(chunk)) > 0) buffer.write(chunk, 0, n);
			return buffer.toByteArray();
		}
	}

	private NeutralizeDelayFix() {
	}
}
