package gg.optic.client.render;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.texture.NativeImageBackedTexture;

/**
 * Fullbright (Lighting): works on the lightmap the game just built, so it can't be capped the
 * way the gamma setting is (OptiFine limits gamma to 100%).
 * - Full Bright on: every light level is fully bright.
 * - Full Bright off: Brightness Boost 1-10 lifts dark levels (1 = normal, 10 = almost full).
 */
public final class OpticFullbright {
	private OpticFullbright() {
	}

	/** Called right after the game updates its lightmap. */
	public static void apply(int[] pixels, NativeImageBackedTexture texture) {
		OpticClient optic = OpticClient.get();
		if (optic == null || pixels == null || texture == null) return;
		OpticConfig c = optic.config();
		if (!c.fullbright) return;
		if (c.fullbrightFull) {
			for (int i = 0; i < pixels.length; i++) pixels[i] = 0xFFFFFFFF;
		} else {
			float boost = Math.max(1.0F, Math.min(10.0F, c.fullbrightBoost));
			if (boost <= 1.001F) return;
			for (int i = 0; i < pixels.length; i++) {
				int p = pixels[i];
				pixels[i] = 0xFF000000 | (lift((p >> 16) & 0xFF, boost) << 16) | (lift((p >> 8) & 0xFF, boost) << 8) | lift(p & 0xFF, boost);
			}
		}
		texture.upload();
	}

	/** Brightens a channel like a gamma curve: 1 - (1 - x)^boost. */
	private static int lift(int channel, float boost) {
		double x = channel / 255.0D;
		double y = 1.0D - Math.pow(1.0D - x, boost);
		return Math.max(0, Math.min(255, (int) Math.round(y * 255.0D)));
	}
}
