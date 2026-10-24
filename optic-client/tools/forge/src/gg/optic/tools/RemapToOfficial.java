package gg.optic.tools;

import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Remaps the Fabric Optic jar from intermediary names onto Mojang 1.8.9 notch names. */
public final class RemapToOfficial {
	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.err.println("usage: RemapToOfficial <mappings.tiny> <input.jar> <output.jar> <classpath.jar>");
			System.exit(2);
		}
		Path tiny = Paths.get(args[0]);
		Path input = Paths.get(args[1]);
		Path output = Paths.get(args[2]);
		Path classpath = Paths.get(args[3]);
		Files.deleteIfExists(output);
		TinyRemapper remapper = TinyRemapper.newRemapper()
			.withMappings(TinyUtils.createTinyMappingProvider(tiny, "intermediary", "official"))
			.ignoreConflicts(true)
			.resolveMissing(true)
			.extension(new MixinExtension())
			.build();
		try (OutputConsumerPath consumer = new OutputConsumerPath.Builder(output).build()) {
			consumer.addNonClassFiles(input);
			remapper.readClassPath(classpath);
			remapper.readInputs(input);
			remapper.apply(consumer);
		} finally {
			remapper.finish();
		}
		System.out.println("remapped " + output);
	}

	private RemapToOfficial() {}
}
