package gg.optic.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

/** Shared HUD geometry for rendering and the edit GUI. */
public final class OpticHudLayout {
	public static final float BASE_KEY = 20.0F;
	public static final float BASE_MOUSE_W = 31.0F;
	public static final float BASE_MOUSE_H = 20.0F;
	public static final float BASE_FONT = 9.0F;
	public static final float HANDLE = 6.0F;
	public static final float GEAR = 10.0F;

	private OpticHudLayout() {
	}

	public static int sx(float base, float scale) {
		return Math.max(1, Math.round(base * scale));
	}

	public static int keystrokesBaseY(OpticConfig c, MinecraftClient client) {
		if (c.keystrokesY >= 0) return c.keystrokesY;
		return new Window(client, client.width, client.height).getHeight() / 2 - gg.optic.client.hud.OpticKeystrokes.height(c) / 2;
	}

	public static int keyX(OpticConfig c, MinecraftClient client, int index) {
		return c.keystrokesX + sx(c.keyOffX(index), c.keystrokesScale);
	}

	public static int keyY(OpticConfig c, MinecraftClient client, int index) {
		return keystrokesBaseY(c, client) + sx(c.keyOffY(index), c.keystrokesScale);
	}

	public static int keyW(OpticConfig c, int index) {
		boolean mouse = index >= 4;
		return sx(mouse ? BASE_MOUSE_W : BASE_KEY, c.keystrokesScale);
	}

	public static int keyH(OpticConfig c, int index) {
		return sx(BASE_MOUSE_H, c.keystrokesScale);
	}

	public static int[] keystrokesBounds(OpticConfig c, MinecraftClient client) {
		return new int[] {
			c.keystrokesX,
			keystrokesBaseY(c, client),
			gg.optic.client.hud.OpticKeystrokes.width(c),
			gg.optic.client.hud.OpticKeystrokes.height(c)
		};
	}

	public static float fontSize(float scale) {
		return BASE_FONT * scale;
	}

	/** Smaller label size when CPS is drawn under LMB/RMB inside the same box. */
	public static float mouseStackedFont(float scale) {
		return Math.max(5.5F, BASE_FONT * 0.72F * scale);
	}

	public static final int HOTBAR_W = 182;
	public static final int HOTBAR_H = 22;

	public static int hotbarX(OpticConfig c, Window window) {
		if (c.hotbarX >= 0) return c.hotbarX;
		return window.getWidth() / 2 - 91;
	}

	public static int hotbarY(OpticConfig c, Window window) {
		if (c.hotbarY >= 0) return c.hotbarY;
		return window.getHeight() - 22;
	}

	public static boolean hotbarVertical(OpticConfig c) {
		int r = OpticConfig.normalizeHotbarRotation(c.hotbarRotation);
		return r == 90 || r == 270;
	}

	public static int hotbarDrawW(OpticConfig c) {
		float scale = Math.max(0.5F, Math.min(3.0F, c.hotbarScale));
		int base = hotbarVertical(c) ? HOTBAR_H : HOTBAR_W;
		return Math.max(1, Math.round(base * scale));
	}

	public static int hotbarDrawH(OpticConfig c) {
		float scale = Math.max(0.5F, Math.min(3.0F, c.hotbarScale));
		int base = hotbarVertical(c) ? HOTBAR_W : HOTBAR_H;
		return Math.max(1, Math.round(base * scale));
	}

	/**
	 * Maps hotbar-local coords (unscaled, 0° layout) into the axis-aligned
	 * box that starts at (hotbarX, hotbarY) after rotation.
	 */
	public static float[] hotbarLocalToBox(float lx, float ly, int rotation) {
		int rot = OpticConfig.normalizeHotbarRotation(rotation);
		switch (rot) {
			case 90:
				return new float[] { HOTBAR_H - ly, lx };
			case 180:
				return new float[] { HOTBAR_W - lx, HOTBAR_H - ly };
			case 270:
				return new float[] { ly, HOTBAR_W - lx };
			default:
				return new float[] { lx, ly };
		}
	}

	/** Placeholder bounds for scoreboard in the edit GUI (live size varies). */
	public static int[] scoreboardEditBounds(OpticConfig c, MinecraftClient client) {
		Window window = new Window(client, client.width, client.height);
		int w = 90;
		int h = 70;
		int x = c.scoreboardX >= 0 ? c.scoreboardX : window.getWidth() - w - 3;
		int y = c.scoreboardY >= 0 ? c.scoreboardY : window.getHeight() / 2 - h / 2;
		return new int[] { x, y, w, h };
	}
}
