package gg.optic.client.hud;

import gg.optic.client.OpticConfig;
import net.minecraft.client.util.Window;

/** Boss Bar mod geometry: vanilla draws a 182px bar at the top center with the name above. */
public final class OpticBossBar {
	public static final int W = 182;
	/** Name text at y=2 down to the bar's bottom at y=17. */
	public static final int H = 17;

	private OpticBossBar() {
	}

	public static float scale(OpticConfig c) {
		return Math.max(0.5F, Math.min(3.0F, c.bossBarScale));
	}

	public static int x(OpticConfig c, Window window) {
		if (c.bossBarX >= 0) return c.bossBarX;
		return window.getWidth() / 2 - Math.round(W * scale(c)) / 2;
	}

	public static int y(OpticConfig c) {
		return c.bossBarY >= 0 ? c.bossBarY : 2;
	}

	public static int drawW(OpticConfig c) {
		return Math.max(1, Math.round(W * scale(c)));
	}

	public static int drawH(OpticConfig c) {
		return Math.max(1, Math.round(H * scale(c)));
	}
}
