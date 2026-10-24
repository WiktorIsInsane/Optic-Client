package gg.optic.tools;

import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Notch 1.8.9 jar -> Legacy Fabric intermediary, used as a classpath for mixin remapping. */
public final class VanillaToIntermediary {
	public static void main(String[] args) throws Exception {
		Path tiny = Paths.get(args[0]);
		Path input = Paths.get(args[1]);
		Path output = Paths.get(args[2]);
		Files.deleteIfExists(output);
		TinyRemapper remapper = TinyRemapper.newRemapper()
			.withMappings(TinyUtils.createTinyMappingProvider(tiny, "official", "intermediary"))
			.ignoreConflicts(true)
			.resolveMissing(true)
			.build();
		try (OutputConsumerPath consumer = new OutputConsumerPath.Builder(output).build()) {
			consumer.addNonClassFiles(input);
			remapper.readInputs(input);
			remapper.apply(consumer);
		} finally {
			remapper.finish();
		}
		System.out.println("intermediary minecraft " + output);
	}
}
