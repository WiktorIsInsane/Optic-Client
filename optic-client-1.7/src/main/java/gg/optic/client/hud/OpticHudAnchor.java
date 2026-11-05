package gg.optic.client.hud;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

/**
 * Keeps HUD mods in the same place on screen when its size changes (windowed <-> fullscreen,
 * resizing, GUI scale). Positions are stored in GUI pixels, so on a size change each placed mod
 * is re-anchored: left/top third keeps its distance from the left/top edge, right/bottom third
 * from the right/bottom edge, and the middle keeps its proportional position.
 */
public final class OpticHudAnchor {
	private OpticHudAnchor() {
	}

	/**
	 * The size the positions belong to is saved with them (hudRefW/H), so a game that starts
	 * small and then goes fullscreen re-anchors from the right size, not the startup window.
	 */
	public static void tick(MinecraftClient client) {
		if (client == null || client.width <= 0 || client.height <= 0) return;
		// Only in-game: the GUI Scale mod changes the scale inside menus, which must not move the HUD.
		if (client.currentScreen != null) return;
		Window window = new Window(client, client.width, client.height);
		int w = window.getWidth();
		int h = window.getHeight();
		OpticConfig c = OpticClient.get().config();
		if (c.hudRefW <= 0 || c.hudRefH <= 0) {
			c.hudRefW = w;
			c.hudRefH = h;
			c.save();
			return;
		}
		if (w == c.hudRefW && h == c.hudRefH) return;
		int ow = c.hudRefW;
		int oh = c.hudRefH;
		c.hudRefW = w;
		c.hudRefH = h;

		c.fpsX = x(c.fpsX, ow, w);
		c.fpsY = y(c.fpsY, oh, h);
		c.cpsX = x(c.cpsX, ow, w);
		c.cpsY = y(c.cpsY, oh, h);
		c.pingX = x(c.pingX, ow, w);
		c.pingY = y(c.pingY, oh, h);
		c.coordinatesX = x(c.coordinatesX, ow, w);
		c.coordinatesY = y(c.coordinatesY, oh, h);
		c.toggleSprintX = x(c.toggleSprintX, ow, w);
		c.toggleSprintY = y(c.toggleSprintY, oh, h);
		c.keystrokesX = x(c.keystrokesX, ow, w);
		c.keystrokesY = y(c.keystrokesY, oh, h);
		c.potionsX = x(c.potionsX, ow, w);
		c.potionsY = y(c.potionsY, oh, h);
		c.titlesX = x(c.titlesX, ow, w);
		c.scoreboardX = x(c.scoreboardX, ow, w);
		c.scoreboardY = y(c.scoreboardY, oh, h);
		c.hotbarX = x(c.hotbarX, ow, w);
		c.hotbarY = y(c.hotbarY, oh, h);
		c.bossBarX = x(c.bossBarX, ow, w);
		c.bossBarY = y(c.bossBarY, oh, h);
		c.save();
	}

	/** Negative values mean "default/automatic placement" and are left alone. */
	private static int x(int value, int oldSize, int newSize) {
		return anchor(value, oldSize, newSize);
	}

	private static int y(int value, int oldSize, int newSize) {
		return anchor(value, oldSize, newSize);
	}

	private static int anchor(int value, int oldSize, int newSize) {
		if (value < 0 || oldSize <= 0) return value;
		int result;
		if (value < oldSize / 3) {
			result = value;
		} else if (value > oldSize * 2 / 3) {
			result = newSize - (oldSize - value);
		} else {
			result = Math.round(value * (newSize / (float) oldSize));
		}
		return Math.max(0, Math.min(Math.max(0, newSize - 2), result));
	}
}
