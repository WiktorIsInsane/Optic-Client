package gg.optic.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;

/**
 * Opening animation for Optic menus: the menu grows from 94% to full size and rises a few
 * pixels into place over about 0.2 s, easing out. Wrap the menu's drawing in begin/end.
 */
final class OpticOpenAnim {
	private static final float SECONDS = 0.2F;
	private long start = -1L;

	/** Restart, e.g. when the menu is opened again. */
	void reset() {
		start = -1L;
	}

	/** 0 at open, 1 when done (eased). */
	float progress() {
		if (start < 0L) start = System.nanoTime();
		float t = Math.min(1.0F, (System.nanoTime() - start) / 1.0E9F / SECONDS);
		float inv = 1.0F - t;
		return 1.0F - inv * inv * inv;
	}

	void begin(float centerX, float centerY) {
		float e = progress();
		float scale = 0.94F + 0.06F * e;
		GlStateManager.pushMatrix();
		GlStateManager.translate(centerX, centerY + 8.0F * (1.0F - e), 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		GlStateManager.translate(-centerX, -centerY, 0.0F);
	}

	void end() {
		GlStateManager.popMatrix();
	}
}
