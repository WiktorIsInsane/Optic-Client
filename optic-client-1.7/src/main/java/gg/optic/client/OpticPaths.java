package gg.optic.client;

import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Stable config locations. Settings used to write under {@code user.dir/optic}, which
 * changes with how the launcher starts Java — so changes looked like they never saved.
 * Prefer {@code %APPDATA%\.optic}, then the Minecraft run directory, then legacy paths.
 */
public final class OpticPaths {
	private static File cachedRoot;
	private static boolean migrated;

	private OpticPaths() {
	}

	public static File root() {
		if (cachedRoot != null) return cachedRoot;
		File preferred = appDataRoot();
		preferred.mkdirs();
		migrateInto(preferred, gameRoot(), legacyUserDirRoot());
		cachedRoot = preferred;
		return cachedRoot;
	}

	public static File settingsFile() {
		return new File(root(), "settings.properties");
	}

	public static File profilesDir() {
		File dir = new File(root(), "profiles");
		if (!dir.isDirectory()) dir.mkdirs();
		return dir;
	}

	/** Extra copies of a profile file, beside the AppData profiles folder. */
	public static void mirrorProfile(File profileFile) {
		if (profileFile == null || !profileFile.isFile()) return;
		File primary;
		try {
			primary = profilesDir().getCanonicalFile();
		} catch (IOException e) {
			primary = profilesDir();
		}
		File[] extras = new File[] { gameProfilesDir(), legacyProfilesDir() };
		for (int i = 0; i < extras.length; i++) {
			File dir = extras[i];
			if (dir == null) continue;
			try {
				if (dir.getCanonicalFile().equals(primary)) continue;
			} catch (IOException ignored) {
			}
			dir.mkdirs();
			copyFile(profileFile, new File(dir, profileFile.getName()));
		}
	}

	private static File gameProfilesDir() {
		File game = gameRoot();
		if (game == null) return null;
		return new File(game, "profiles");
	}

	private static File legacyProfilesDir() {
		return new File(legacyUserDirRoot(), "profiles");
	}

	/** Call once client is up so gameDir configs migrate into AppData. */
	public static void refreshAfterClientReady() {
		File game = gameRoot();
		File preferred = appDataRoot();
		if (game == null) return;
		preferred.mkdirs();
		migrateInto(preferred, game, legacyUserDirRoot());
		cachedRoot = preferred;
	}

	private static File appDataRoot() {
		String appData = System.getenv("APPDATA");
		if (appData == null || appData.isEmpty()) {
			appData = System.getProperty("user.home", ".");
		}
		return new File(appData, ".optic");
	}

	private static File gameRoot() {
		try {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client != null && client.runDirectory != null) {
				return new File(client.runDirectory, "optic");
			}
		} catch (Throwable ignored) {
		}
		return null;
	}

	private static File legacyUserDirRoot() {
		return new File(new File(System.getProperty("user.dir", ".")), "optic");
	}

	private static void migrateInto(File dest, File... sources) {
		if (migrated) return;
		migrated = true;
		dest.mkdirs();
		File destSettings = new File(dest, "settings.properties");
		for (File source : sources) {
			if (source == null || source.equals(dest) || !source.isDirectory()) continue;
			File srcSettings = new File(source, "settings.properties");
			if (!destSettings.isFile() && srcSettings.isFile()) {
				copyFile(srcSettings, destSettings);
			}
			File srcProfiles = new File(source, "profiles");
			File destProfiles = new File(dest, "profiles");
			if (srcProfiles.isDirectory()) {
				destProfiles.mkdirs();
				File[] files = srcProfiles.listFiles();
				if (files != null) {
					for (File file : files) {
						if (!file.isFile()) continue;
						File target = new File(destProfiles, file.getName());
						if (!target.isFile()) copyFile(file, target);
					}
				}
			}
		}
	}

	private static void copyFile(File from, File to) {
		try {
			Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ignored) {
		}
	}
}
