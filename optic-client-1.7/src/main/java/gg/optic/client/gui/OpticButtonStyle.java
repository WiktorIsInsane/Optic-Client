package gg.optic.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticUiDraw;
import net.minecraft.client.MinecraftClient;

/**
 * The look for every Minecraft-made button and slider: a fully transparent box with only a thin dark
 * rim, independent of the resource pack's widgets texture. Text stays vanilla.
 */
public final class OpticButtonStyle {
	private static final int RIM = 0xFF0B0B0C;
	private static final int RIM_HOT = 0xFFB4B4B8;
	private static final int RADIUS = 3;

	private OpticButtonStyle() {
	}

	/** state: 0 disabled, 1 normal, 2 hovered (vanilla getYImage values). */
	public static void box(MinecraftClient client, int x, int y, int w, int h, int state) {
		if (w <= 0 || h <= 0) return;
		// Fully transparent: no fill, only the rim.
		OpticUiDraw.outlineExact(client, x, y, w, h, RADIUS, state == 2 ? RIM_HOT : RIM);
		GlStateManager.enableTexture();
		GlStateManager.enableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/** Slider handle: a narrow box in the same style. */
	public static void knob(MinecraftClient client, int x, int y, int w, int h, boolean hot) {
		OpticUiDraw.outlineExact(client, x, y, w, h, 2, hot ? 0xFFC8C8CC : 0xFF0B0B0C);
		GlStateManager.enableTexture();
		GlStateManager.enableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}
}