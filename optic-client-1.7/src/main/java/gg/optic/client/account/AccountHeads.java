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

/** Downloads and caches 32px head avatars from mc-heads.net. */
public final class AccountHeads {
	private static final Map<String, Identifier> CACHE = new HashMap<String, Identifier>();
	private static final Set<String> LOADING = new HashSet<String>();

	private AccountHeads() {
	}

	public static Identifier get(MinecraftClient client, String uuid) {
		if (uuid == null || uuid.isEmpty()) return null;
		String key = uuid.replace("-", "").toLowerCase();
		Identifier cached = CACHE.get(key);
		if (cached != null) return cached;
		if (!LOADING.contains(key)) {
			LOADING.add(key);
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						BufferedImage image = download(key);
						if (image == null) return;
						final BufferedImage ready = image;
						client.execute(new Callable<Object>() {
							@Override
							public Object call() {
								try {
									NativeImageBackedTexture texture = new NativeImageBackedTexture(ready);
									gg.optic.client.compat.Mc17.setFilter(texture, true, true);
									Identifier id = client.getTextureManager().registerDynamicTexture("optic_head_" + key, texture);
									Texture bound = client.getTextureManager().getTexture(id);
									if (bound instanceof AbstractTexture) {
										gg.optic.client.compat.Mc17.setFilter((AbstractTexture) bound, true, true);
									}
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

	private static BufferedImage download(String uuidNoDashes) throws Exception {
		URL url = new URL("https://mc-heads.net/avatar/" + uuidNoDashes + "/128");
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
