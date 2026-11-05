package gg.optic.client.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cosmetics shared with the launcher at {@code %APPDATA%\.optic\shared\cosmetics.json}.
 * The launcher owns the catalog; both sides read and write what is equipped, so a change
 * in either one shows up in the other.
 */
public final class SharedCosmetics {
	/** Same ids and order as the launcher's Cosmetics tabs. */
	public static final String[] CATEGORIES = { "icons", "capes", "wings", "bandannas" };
	public static final String[] LABELS = { "Icons", "Capes", "Wings", "Bandannas" };

	public static final class Item {
		public final String id;
		public final String name;

		Item(String id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	private static final Map<String, List<Item>> catalog = new LinkedHashMap<String, List<Item>>();
	private static final Map<String, String> equipped = new LinkedHashMap<String, String>();
	private static long loadedStamp = -1L;
	private static long lastCheck;

	private SharedCosmetics() {
	}

	public static File sharedFile() {
		// Same folder as the shared accounts, so .optic vs legacy .lunar-remake matches the launcher.
		return new File(gg.optic.client.account.SharedAccounts.sharedFile().getParentFile(), "cosmetics.json");
	}

	/** Re-reads the file when the launcher changed it (checked at most twice a second). */
	public static void refresh() {
		long now = System.currentTimeMillis();
		if (now - lastCheck < 500L && loadedStamp != -1L) return;
		lastCheck = now;
		File file = sharedFile();
		long stamp = file.isFile() ? file.lastModified() ^ file.length() : 0L;
		if (stamp == loadedStamp) return;
		loadedStamp = stamp;
		load(file);
	}

	public static List<Item> items(String category) {
		refresh();
		List<Item> list = catalog.get(category);
		return list == null ? Collections.<Item>emptyList() : list;
	}

	/** Equipped item id, or "" for none. */
	public static String equipped(String category) {
		refresh();
		String id = equipped.get(category);
		return id == null ? "" : id;
	}

	public static void equip(String category, String id) {
		refresh();
		equipped.put(category, id == null ? "" : id);
		save();
	}

	/** Items this client can draw. Always listed, whatever the launcher's catalog says. */
	private static void addBuiltIns() {
		addBuiltIn("capes", OpticCosmetics.CAPE_OPTIC, "Optic Cape");
		addBuiltIn("wings", OpticCosmetics.WINGS_DRAGON, "Ender Dragon Wings");
	}

	private static void addBuiltIn(String category, String id, String name) {
		List<Item> list = catalog.get(category);
		if (list == null) {
			list = new ArrayList<Item>();
			catalog.put(category, list);
		}
		for (Item item : list) {
			if (item.id.equals(id)) return;
		}
		list.add(0, new Item(id, name));
	}

	private static void load(File file) {
		catalog.clear();
		equipped.clear();
		try {
			read(file);
		} finally {
			addBuiltIns();
		}
	}

	private static void read(File file) {
		if (!file.isFile()) return;
		try {
			JsonObject root = new JsonParser().parse(readAll(file)).getAsJsonObject();
			JsonObject cat = root.has("catalog") && root.get("catalog").isJsonObject() ? root.getAsJsonObject("catalog") : null;
			JsonObject eq = root.has("equipped") && root.get("equipped").isJsonObject() ? root.getAsJsonObject("equipped") : null;
			for (String category : CATEGORIES) {
				List<Item> list = new ArrayList<Item>();
				if (cat != null && cat.has(category) && cat.get(category).isJsonArray()) {
					for (JsonElement el : cat.getAsJsonArray(category)) {
						if (!el.isJsonObject()) continue;
						JsonObject o = el.getAsJsonObject();
						String id = str(o, "id");
						if (id.isEmpty()) continue;
						String name = str(o, "name");
						list.add(new Item(id, name.isEmpty() ? id : name));
					}
				}
				catalog.put(category, list);
				equipped.put(category, eq == null ? "" : str(eq, category));
			}
		} catch (Exception e) {
			System.err.println("[Optic] Shared cosmetics read failed: " + e.getMessage());
		}
	}

	private static void save() {
		File file = sharedFile();
		JsonObject root;
		try {
			root = file.isFile() ? new JsonParser().parse(readAll(file)).getAsJsonObject() : new JsonObject();
		} catch (Exception e) {
			root = new JsonObject();
		}
		// Keep the launcher's catalog as it is; only the equipped map is ours to write.
		if (!root.has("catalog")) {
			JsonObject cat = new JsonObject();
			for (String category : CATEGORIES) cat.add(category, new JsonArray());
			root.add("catalog", cat);
		}
		JsonObject eq = new JsonObject();
		for (String category : CATEGORIES) {
			String id = equipped.get(category);
			eq.addProperty(category, id == null ? "" : id);
		}
		root.addProperty("version", 1);
		root.add("equipped", eq);
		root.addProperty("updatedAt", System.currentTimeMillis());
		try {
			file.getParentFile().mkdirs();
			File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
			OutputStream out = new FileOutputStream(tmp);
			try {
				out.write(root.toString().getBytes(StandardCharsets.UTF_8));
			} finally {
				out.close();
			}
			if (!tmp.renameTo(file)) {
				file.delete();
				if (!tmp.renameTo(file)) throw new java.io.IOException("rename failed");
			}
			loadedStamp = file.lastModified() ^ file.length();
		} catch (Exception e) {
			System.err.println("[Optic] Shared cosmetics write failed: " + e.getMessage());
		}
	}

	private static String str(JsonObject o, String key) {
		if (!o.has(key) || o.get(key).isJsonNull()) return "";
		try {
			return o.get(key).getAsString();
		} catch (Exception e) {
			return "";
		}
	}

	private static String readAll(File file) throws java.io.IOException {
		InputStream in = new FileInputStream(file);
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			byte[] chunk = new byte[4096];
			int n;
			while ((n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
			return new String(buf.toByteArray(), StandardCharsets.UTF_8);
		} finally {
			in.close();
		}
	}
}
