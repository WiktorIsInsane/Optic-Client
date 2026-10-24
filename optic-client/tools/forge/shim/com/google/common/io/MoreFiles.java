package com.google.common.io;

import java.io.IOException;
import java.nio.file.Path;

public final class MoreFiles {
	private MoreFiles() {}

	public static void deleteRecursively(Path path, RecursiveDeleteOption... options) throws IOException {
		if (path != null && !java.nio.file.Files.exists(path)) {
			throw new IOException(path.toString());
		}
	}
}
