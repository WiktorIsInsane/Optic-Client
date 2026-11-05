package gg.optic.client.account;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.Texture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
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
		String id = uuid != null && !uuid.isEmpty() ? uuid.replace("-", "").toLowerCase() : null;
		if (id == null && name != null && !name.isEmpty()) id = name;
		if (id == null) return null;
		final String fallbackName = name;
		final String key = id;
		Identifier cached = CACHE.get(key);
		if (cached != null) return cached;
		if (!LOADING.contains(key)) {
			LOADING.add(key);
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						BufferedImage skin = download(key);
						if (skin == null && fallbackName != null && !fallbackName.equalsIgnoreCase(key)) skin = download(fallbackName);
						BufferedImage image = skin == null ? null : face(skin);
						if (image == null) {
							System.err.println("[Optic] No skin found for " + key);
							return;
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

	private static BufferedImage download(String who) {
		try {
			return fetch(who);
		} catch (Exception e) {
			System.err.println("[Optic] Skin download failed for " + who + ": " + e);
			return null;
		}
	}

	private static BufferedImage fetch(String who) throws Exception {
		URL url = new URL("https://mc-heads.net/skin/" + who);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(4000);
		conn.setReadTimeout(4000);
		conn.setRequestProperty("User-Agent", "OpticClient/1.0");
		conn.connect();
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
