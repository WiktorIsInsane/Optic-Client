package gg.optic.tools;

import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Remaps the Legacy Fabric Optic jar (intermediary names) onto Forge 1.8.9 SRG names,
 * including mixin targets, so the same client can load beside OptiFine.
 */
public final class RemapToForge {
	public static void main(String[] args) throws Exception {
		if (args.length < 5) {
			System.err.println("usage: RemapToForge <intermediary.tiny> <joined.srg> <input.jar> <output.jar> <out.tiny> [classpath.jar]");
			System.exit(2);
		}
		Path tiny = Paths.get(args[0]);
		Path srg = Paths.get(args[1]);
		Path input = Paths.get(args[2]);
		Path output = Paths.get(args[3]);
		Path outTiny = Paths.get(args[4]);
		Path classpath = args.length > 5 ? Paths.get(args[5]) : null;
		writeMapping(tiny, srg, outTiny);
		remap(input, output, outTiny, classpath);
		System.out.println("remapped " + output);
	}

	private static void writeMapping(Path tinyPath, Path srgPath, Path out) throws IOException {
		Map<String, String> officialToInter = new HashMap<String, String>();
		Map<String, String> interToOfficialClass = new HashMap<String, String>();
		Map<String, ClassBody> classes = new LinkedHashMap<String, ClassBody>();

		String currentOfficial = null;
		for (String raw : Files.readAllLines(tinyPath, StandardCharsets.UTF_8)) {
			if (raw.startsWith("tiny\t") || raw.isEmpty()) continue;
			String[] p = raw.split("\t", -1);
			if (p.length >= 3 && "c".equals(p[0])) {
				currentOfficial = p[1];
				officialToInter.put(p[1], p[2]);
				interToOfficialClass.put(p[2], p[1]);
				classes.put(p[2], new ClassBody());
			} else if (currentOfficial != null && p.length >= 5 && "m".equals(p[1])) {
				classes.get(officialToInter.get(currentOfficial)).methods.add(new Member(p[2], p[3], p[4]));
			} else if (currentOfficial != null && p.length >= 5 && "f".equals(p[1])) {
				classes.get(officialToInter.get(currentOfficial)).fields.add(new Member(p[2], p[3], p[4]));
			}
		}

		Map<String, String> officialToSrgClass = new HashMap<String, String>();
		Map<String, String> methodToSrg = new HashMap<String, String>();
		Map<String, String> fieldToSrg = new HashMap<String, String>();
		for (String raw : Files.readAllLines(srgPath, StandardCharsets.UTF_8)) {
			if (raw.startsWith("CL: ")) {
				String[] p = raw.substring(4).split(" ");
				officialToSrgClass.put(p[0], p[1]);
			} else if (raw.startsWith("MD: ")) {
				String[] p = raw.substring(4).split(" ");
				int slash = p[2].lastIndexOf('/');
				methodToSrg.put(p[0] + " " + p[1], p[2].substring(slash + 1));
			} else if (raw.startsWith("FD: ")) {
				String[] p = raw.substring(4).split(" ");
				int slash = p[1].lastIndexOf('/');
				fieldToSrg.put(p[0], p[1].substring(slash + 1));
			}
		}

		BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8);
		try {
			w.write("tiny\t2\t0\tintermediary\tsrg\n");
			for (Map.Entry<String, String> e : officialToInter.entrySet()) {
				String srgName = officialToSrgClass.containsKey(e.getKey()) ? officialToSrgClass.get(e.getKey()) : e.getKey();
				w.write("c\t" + e.getValue() + "\t" + srgName + "\n");
				ClassBody body = classes.get(e.getValue());
				if (body == null) continue;
				for (Member m : body.methods) {
					String desc = remapDesc(m.desc, officialToInter);
					String target = methodToSrg.get(e.getKey() + "/" + m.officialName + " " + m.desc);
					if (target == null) target = m.officialName;
					w.write("\tm\t" + desc + "\t" + m.interName + "\t" + target + "\n");
				}
				for (Member f : body.fields) {
					String desc = remapDesc(f.desc, officialToInter);
					String target = fieldToSrg.get(e.getKey() + "/" + f.officialName);
					if (target == null) target = f.officialName;
					w.write("\tf\t" + desc + "\t" + f.interName + "\t" + target + "\n");
				}
			}
		} finally {
			w.close();
		}
		System.out.println("classes " + officialToInter.size() + " methods " + methodToSrg.size());
	}

	private static String remapDesc(String desc, Map<String, String> officialToInter) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < desc.length(); i++) {
			char c = desc.charAt(i);
			if (c == 'L') {
				int end = desc.indexOf(';', i);
				String official = desc.substring(i + 1, end);
				String inter = officialToInter.containsKey(official) ? officialToInter.get(official) : official;
				sb.append('L').append(inter).append(';');
				i = end;
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private static void remap(Path input, Path output, Path mapping, Path classpath) throws IOException {
		Files.deleteIfExists(output);
		TinyRemapper remapper = TinyRemapper.newRemapper()
			.withMappings(TinyUtils.createTinyMappingProvider(mapping, "intermediary", "srg"))
			.ignoreConflicts(true)
			.resolveMissing(true)
			.extension(new MixinExtension())
			.build();
		try (OutputConsumerPath consumer = new OutputConsumerPath.Builder(output).build()) {
			consumer.addNonClassFiles(input);
			if (classpath != null) remapper.readClassPath(classpath);
			remapper.readInputs(input);
			remapper.apply(consumer);
		} finally {
			remapper.finish();
		}
	}

	private static final class ClassBody {
		final java.util.List<Member> methods = new java.util.ArrayList<Member>();
		final java.util.List<Member> fields = new java.util.ArrayList<Member>();
	}

	private static final class Member {
		final String desc;
		final String officialName;
		final String interName;

		Member(String desc, String officialName, String interName) {
			this.desc = desc;
			this.officialName = officialName;
			this.interName = interName;
		}
	}
}
