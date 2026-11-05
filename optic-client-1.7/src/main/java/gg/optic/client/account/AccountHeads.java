package gg.optic.client.account;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.Texture;
import net.minecraft.util.Identifier;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/** Downloads the player's real skin and cuts out the face (with the hat layer), kept pixel-sharp. */
public final class AccountHeads {
	private static final Map<String, Identifier> CACHE = new HashMap<String, Identifier>();
	private static final Set<String> LOADING = new HashSet<String>();

	private AccountHeads() {
	}

	public static Identifier get(MinecraftClient client, String uuid) {
		return get(client, uuid, null);
	}

	/** Uses the uuid when there is one, the username otherwise (offline accounts with a real name). */
	public static Identifier get(MinecraftClient client, String uuid, String name) {
		return load(client, uuid, name, false);
	}

	/** The whole skin as a 64x32 texture (1.7 player model layout), or null while it downloads. */
	public static Identifier skin(MinecraftClient client, String uuid, String name) {
		// By username first: offline accounts carry a made-up uuid that has no skin.
		if (name != null && !name.isEmpty()) return load(client, null, name, true);
		return load(client, uuid, null, true);
	}

	/** Whether that skin (same arguments as {@link #skin}) uses slim 3px arms; false until loaded. */
	public static boolean slim(String uuid, String name) {
		String id = name != null && !name.isEmpty() ? name : (uuid == null ? "" : uuid.replace("-", "").toLowerCase());
		return Boolean.TRUE.equals(SLIM.get("skin_" + id));
	}

	private static final Map<String, Boolean> SLIM = new java.util.concurrent.ConcurrentHashMap<String, Boolean>();
	/** Arm model Mojang reported for a lookup ("who" lowercased), when it came from Mojang. */
	private static final Map<String, Boolean> MOJANG_SLIM = new java.util.concurrent.ConcurrentHashMap<String, Boolean>();

	/** Slim or not for a username once its Mojang lookup finished, else null. */
	public static Boolean slimIfKnown(String name) {
		if (name == null || name.isEmpty()) return null;
		Boolean done = SLIM.get("skin_" + name);
		if (done != null) return done;
		// No custom skin: Mojang answered, but there was nothing to download.
		return MOJANG_SLIM.get(name.toLowerCase());
	}

	private static Identifier load(final MinecraftClient client, String uuid, String name, final boolean whole) {
		String id = uuid != null && !uuid.isEmpty() ? uuid.replace("-", "").toLowerCase() : null;
		if (id == null && name != null && !name.isEmpty()) id = name;
		if (id == null) return null;
		final String fallbackName = name;
		final String key = (whole ? "skin_" : "") + id;
		final String who = id;
		Identifier cached = CACHE.get(key);
		if (cached != null) return cached;
		if (!LOADING.contains(key)) {
			LOADING.add(key);
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						BufferedImage skin = download(who);
						if (skin == null && fallbackName != null && !fallbackName.equalsIgnoreCase(who)) skin = download(fallbackName);
						BufferedImage image = skin == null ? null : (whole ? legacy(skin) : face(skin));
						if (image == null) {
							System.err.println("[Optic] No skin found for " + key);
							return;
						}
						if (whole) {
							Boolean reported = MOJANG_SLIM.get(who.toLowerCase());
							// Only Mojang knows the arm model; mc-heads fallbacks count as classic.
							SLIM.put(key, reported != null ? reported : Boolean.FALSE);
						}
						final BufferedImage ready = image;
						client.execute(new Callable<Object>() {
							@Override
							public Object call() {
								try {
									NativeImageBackedTexture texture = new NativeImageBackedTexture(ready);
									Identifier id = client.getTextureManager().registerDynamicTexture("optic_head_" + key.toLowerCase(), texture);
									CACHE.put(key, id);
								} catch (Exception e) {
									System.err.println("[Optic] Head upload failed: " + e.getMessage());
								} finally {
									LOADING.remove(key);
								}
								return null;
							}
						});
					} catch (Exception e) {
						LOADING.remove(key);
						System.err.println("[Optic] Head download failed: " + e.getMessage());
					}
				}
			}, "optic-head-" + key.substring(0, Math.min(8, key.length())));
			thread.setDaemon(true);
			thread.start();
		}
		return null;
	}

	/**
	 * Face (8,8) with the hat layer (40,8) on top, scaled up 16x with nearest-neighbour so each
	 * skin pixel stays a crisp square: 128x128, the size callers draw from.
	 */
	/** Top half of a 64x64 (or HD) skin at 64x32: the only part 1.7's player model reads. */
	private static BufferedImage legacy(BufferedImage skin) {
		if (skin.getWidth() < 64 || skin.getHeight() < 32) return null;
		int scale = skin.getWidth() / 64;
		BufferedImage out = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 32; y++) {
			for (int x = 0; x < 64; x++) out.setRGB(x, y, skin.getRGB(x * scale, y * scale));
		}
		return out;
	}

	private static BufferedImage face(BufferedImage skin) {
		if (skin.getWidth() < 64 || skin.getHeight() < 32) return null;
		int scale = skin.getWidth() / 64;
		BufferedImage out = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				int base = skin.getRGB((8 + x) * scale, (8 + y) * scale) | 0xFF000000;
				int hat = skin.getRGB((40 + x) * scale, (8 + y) * scale);
				int a = (hat >>> 24) & 0xFF;
				int px = a >= 128 ? (hat | 0xFF000000) : base;
				for (int dy = 0; dy < 16; dy++) {
					for (int dx = 0; dx < 16; dx++) out.setRGB(x * 16 + dx, y * 16 + dy, px);
				}
			}
		}
		return out;
	}

	/** Mojang first (always current), mc-heads.net as a fallback (its name lookups can be stale). */
	private static BufferedImage download(String who) {
		try {
			BufferedImage skin = fetchMojang(who);
			if (skin != null) return skin;
		} catch (Exception e) {
			System.err.println("[Optic] Mojang skin lookup failed for " + who + ": " + e);
		}
		try {
			return readImage("https://mc-heads.net/skin/" + who);
		} catch (Exception e) {
			System.err.println("[Optic] Skin download failed for " + who + ": " + e);
			return null;
		}
	}

	/** Name or uuid -> profile -> skin texture. Records whether it is a slim-arm skin. */
	private static BufferedImage fetchMojang(String who) throws Exception {
		String uuid = who.replace("-", "");
		if (!uuid.matches("[0-9a-fA-F]{32}")) {
			String body = readText("https://api.mojang.com/users/profiles/minecraft/" + who);
			if (body == null) return null;
			uuid = new JsonParser().parse(body).getAsJsonObject().get("id").getAsString();
		}
		String profile = readText("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
		if (profile == null) return null;
		JsonObject root = new JsonParser().parse(profile).getAsJsonObject();
		for (com.google.gson.JsonElement el : root.getAsJsonArray("properties")) {
			JsonObject prop = el.getAsJsonObject();
			if (!"textures".equals(prop.get("name").getAsString())) continue;
			String decoded = new String(java.util.Base64.getDecoder().decode(prop.get("value").getAsString()), StandardCharsets.UTF_8);
			JsonObject textures = new JsonParser().parse(decoded).getAsJsonObject().getAsJsonObject("textures");
			if (textures == null || !textures.has("SKIN")) {
				// No custom skin: the default for this account.
				MOJANG_SLIM.put(who.toLowerCase(), Boolean.FALSE);
				return null;
			}
			JsonObject skin = textures.getAsJsonObject("SKIN");
			boolean slim = skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")
				&& "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());
			MOJANG_SLIM.put(who.toLowerCase(), Boolean.valueOf(slim));
			return readImage(skin.get("url").getAsString().replace("http://", "https://"));
		}
		return null;
	}

	private static HttpURLConnection open(String address) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
		conn.setConnectTimeout(4000);
		conn.setReadTimeout(4000);
		conn.setRequestProperty("User-Agent", "OpticClient/1.0");
		return conn;
	}

	/** Body text, or null when the server has nothing for it (e.g. unknown name). */
	private static String readText(String address) throws Exception {
		HttpURLConnection conn = open(address);
		try {
			if (conn.getResponseCode() != 200) return null;
			InputStream in = conn.getInputStream();
			try {
				java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
				byte[] buf = new byte[4096];
				int n;
				while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
				return new String(out.toByteArray(), StandardCharsets.UTF_8);
			} finally {
				in.close();
			}
		} finally {
			conn.disconnect();
		}
	}

	private static BufferedImage readImage(String address) throws Exception {
		HttpURLConnection conn = open(address);
		try {
			InputStream in = conn.getInputStream();
			try {
				return ImageIO.read(in);
			} finally {
				in.close();
			}
		} finally {
			conn.disconnect();
		}
	}
}
