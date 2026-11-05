package gg.optic.client.gui;

import gg.optic.client.OpticUiDraw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;

/**
 * One place for the Optic menu look (mod menu, main menu, buttons). Tweak colors and sizes
 * here; the screens only reference these names.
 *
 * Style: Lunar-like frosted glass and soft shadows, CheatBreaker-like accent edge on the
 * active/hovered thing plus small uppercase section labels, Badlion-like sidebar + cards.
 */
public final class OpticTheme {
	// Accent: Optic green fading into teal for highlight lines.
	public static final int ACCENT = 0xFF5CFF8A;
	public static final int ACCENT_2 = 0xFF2EE6C8;
	public static final int ACCENT_DIM = 0xFF2FD66A;
	public static final int ACCENT_SOFT = 0x2E5CFF8A;
	public static final int ACCENT_GLOW = 0x165CFF8A;
	/** Text drawn on top of a solid accent fill. */
	public static final int ON_ACCENT = 0xFF06120A;

	// Surfaces (dark glass, alpha lets the blur show through a little).
	public static final int SCRIM = 0xB008080A;
	public static final int PANEL = 0xE40C0D11;
	public static final int TOPBAR = 0xF0101116;
	public static final int SIDEBAR = 0xF00A0B0E;
	public static final int CARD = 0xB4131419;
	public static final int CARD_HOVER = 0xD21B1D24;
	public static final int CARD_ON = 0xC0111A15;
	public static final int TAB_IDLE = 0xFF15161B;
	public static final int SHADOW = 0x55000000;

	// Lines and text.
	// Outlines are solid colors: see-through ones blend twice where the rounded-corner pieces
	// overlap and look broken.
	public static final int BORDER = 0xFF25272E;
	public static final int BORDER_SOFT = 0xFF1C1E23;
	/** Outline of an enabled card / active tab: solid dark green. */
	public static final int BORDER_ON = 0xFF2C6B45;
	public static final int HIGHLIGHT = 0x0FFFFFFF;
	public static final int TEXT = 0xFFF2F4F7;
	public static final int MUTED = 0xFF8C93A1;
	public static final int FAINT = 0xFF5C6270;

	// Main menu buttons.
	public static final int BUTTON = 0x8C0B0C10;
	public static final int BUTTON_HOVER = 0xC0102118;
	public static final int BUTTON_OFF = 0x660B0C10;

	public static final int RADIUS = 7;
	public static final int RADIUS_SM = 4;

	private OpticTheme() {
	}

	/** Horizontal line fading accent -> teal -> transparent (CheatBreaker-style header rule). */
	public static void accentLine(int x, int y, int w, int h) {
		if (w <= 0 || h <= 0) return;
		int steps = Math.max(1, Math.min(48, w / 4));
		for (int i = 0; i < steps; i++) {
			int x1 = x + w * i / steps;
			int x2 = x + w * (i + 1) / steps;
			float t = i / (float) Math.max(1, steps - 1);
			int rgb = lerp(ACCENT, ACCENT_2, Math.min(1.0F, t * 1.6F)) & 0xFFFFFF;
			int alpha = Math.round(255 * (t < 0.6F ? 1.0F : 1.0F - (t - 0.6F) / 0.4F));
			DrawableHelper.fill(x1, y, x2, y + h, (Math.max(0, alpha) << 24) | rgb);
		}
	}

	/** Thin accent strip on the left edge of an active or hovered element. */
	public static void accentEdge(int x, int y, int h, boolean strong) {
		DrawableHelper.fill(x, y + 2, x + 2, y + h - 2, strong ? ACCENT : 0x885CFF8A);
	}

	/** Rounded glass surface with a faint top highlight (Lunar-style). */
	public static void glass(MinecraftClient client, int x, int y, int w, int h, int radius, int fill, int border) {
		OpticUiDraw.fillExact(client, x, y, w, h, radius, fill);
		DrawableHelper.fill(x + radius, y + 1, x + w - radius, y + 2, HIGHLIGHT);
		if (((border >>> 24) & 0xFF) != 0) OpticUiDraw.outlineExact(client, x, y, w, h, radius, border);
	}

	/** Two-layer soft drop shadow under a panel. */
	public static void shadow(MinecraftClient client, int x, int y, int w, int h, int radius) {
		OpticUiDraw.fillExact(client, x + 3, y + 6, w, h, radius + 2, 0x40000000);
		OpticUiDraw.fillExact(client, x + 1, y + 3, w, h, radius + 1, 0x30000000);
	}

	public static int lerp(int a, int b, float t) {
		t = Math.max(0.0F, Math.min(1.0F, t));
		int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		return (Math.round(aa + (ba - aa) * t) << 24)
			| (Math.round(ar + (br - ar) * t) << 16)
			| (Math.round(ag + (bg - ag) * t) << 8)
			| Math.round(ab + (bb - ab) * t);
	}
}
