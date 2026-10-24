package gg.optic.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Shareable JSON profiles for mods, the Settings tab, colors, and HUD layout. */
public final class OpticProfiles {
	private OpticProfiles() {
	}

	public static File folder() {
		return OpticPaths.profilesDir();
	}

	public static List<File> list() {
		File dir = folder();
		File[] files = dir.listFiles();
		List<File> out = new ArrayList<File>();
		if (files == null) return out;
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File a, File b) {
				return a.getName().compareToIgnoreCase(b.getName());
			}
		});
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			String name = file.getName().toLowerCase();
			if (file.isFile() && (name.endsWith(".json") || name.endsWith(".txt"))) {
				out.add(file);
			}
		}
		return out;
	}

	public static List<File> listFiltered(String query) {
		List<File> all = list();
		if (query == null || query.trim().isEmpty()) return all;
		String q = query.trim().toLowerCase();
		List<File> out = new ArrayList<File>();
		for (int i = 0; i < all.size(); i++) {
			File file = all.get(i);
			if (displayName(file).toLowerCase().contains(q)) out.add(file);
		}
		return out;
	}

	public static String sanitizeName(String raw) {
		if (raw == null) return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == ' ') {
				sb.append(c == ' ' ? '_' : c);
			}
		}
		String cleaned = sb.toString().replaceAll("_+", "_");
		if (cleaned.isEmpty()) cleaned = "profile";
		if (cleaned.length() > 32) cleaned = cleaned.substring(0, 32);
		return cleaned;
	}

	public static File fileFor(String name) {
		return new File(folder(), sanitizeName(name) + ".json");
	}

	/** Write profile JSON without touching settings.properties. */
	public static File write(OpticConfig config, String name) throws IOException {
		String safe = sanitizeName(name);
		File file = new File(folder(), safe + ".json");
		JsonObject root = config.toProfileJson();
		root.addProperty("name", safe);
		root.addProperty("format", 1);
		root.addProperty("client", "Optic");
		FileWriter writer = new FileWriter(file);
		try {
			writer.write(root.toString());
		} finally {
			writer.close();
		}
		OpticPaths.mirrorProfile(file);
		return file;
	}

	public static File save(OpticConfig config, String name) throws IOException {
		String safe = sanitizeName(name);
		File file = write(config, safe);
		config.activeProfile = safe;
		// Persist settings.properties only (save() would rewrite the profile again).
		config.saveSettingsOnly();
		return file;
	}

	/**
	 * With no other profile files, Default is always the selected one and is written into the profiles folder.
	 * If other profiles exist, the last selected file is kept when it is still there.
	 */
	public static void ensureSelected(OpticConfig config) throws IOException {
		List<File> others = new ArrayList<File>();
		List<File> files = list();
		for (int i = 0; i < files.size(); i++) {
			if (!"default".equalsIgnoreCase(displayName(files.get(i)))) others.add(files.get(i));
		}
		if (others.isEmpty()) {
			config.activeProfile = "default";
			write(config, "default");
			return;
		}
		String name = config.activeProfile == null ? "" : config.activeProfile.trim();
		if (!name.isEmpty()) {
			File current = fileFor(name);
			if (current.isFile()) {
				config.activeProfile = displayName(current);
				return;
			}
		}
		File fallback = fileFor("default");
		if (fallback.isFile()) {
			load(config, fallback);
			return;
		}
		load(config, others.get(0));
	}

	public static void load(OpticConfig config, File file) throws IOException {
		FileReader reader = new FileReader(file);
		try {
			JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
			config.applyProfileJson(root);
			config.activeProfile = displayName(file);
			config.saveSettingsOnly();
			// Sync active profile file with applied state.
			write(config, config.activeProfile);
		} finally {
			reader.close();
		}
	}

	public static boolean delete(File file) {
		return file != null && file.isFile() && file.delete();
	}

	public static File duplicate(File file) throws IOException {
		String base = displayName(file);
		String name = base + "_copy";
		File dest = fileFor(name);
		int n = 2;
		while (dest.isFile()) {
			name = base + "_copy" + n;
			dest = fileFor(name);
			n++;
		}
		copyFile(file, dest);
		return dest;
	}

	public static File rename(File file, String newName) throws IOException {
		File dest = fileFor(newName);
		if (dest.equals(file)) return file;
		if (dest.isFile()) throw new IOException("A profile named that already exists");
		if (!file.renameTo(dest)) {
			copyFile(file, dest);
			if (!file.delete()) {
				dest.delete();
				throw new IOException("Could not rename profile");
			}
		}
		return dest;
	}

	public static void openFolder() {
		try {
			File dir = folder();
			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().open(dir);
			}
		} catch (Exception ignored) {
		}
	}

	public static String displayName(File file) {
		String name = file.getName();
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : name;
	}

	public static String uniqueNewName() {
		String base = "Profile";
		File dest = fileFor(base);
		int n = 1;
		while (dest.isFile()) {
			n++;
			dest = fileFor(base + n);
		}
		return displayName(dest);
	}

	private static void copyFile(File from, File to) throws IOException {
		FileInputStream in = new FileInputStream(from);
		try {
			FileOutputStream out = new FileOutputStream(to);
			try {
				byte[] buf = new byte[4096];
				int n;
				while ((n = in.read(buf)) >= 0) {
					out.write(buf, 0, n);
				}
			} finally {
				out.close();
			}
		} finally {
			in.close();
		}
	}
}
