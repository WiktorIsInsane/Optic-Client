package gg.optic.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticClient;
import gg.optic.client.OpticTextures;
import gg.optic.client.OpticUiDraw;
import gg.optic.client.OpticUiFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/** Same layout as the reference menu, drawn in the Optic green theme at high resolution. */
public final class OpticMenuChrome {
	private static final int ACCENT = OpticTheme.ACCENT;
	/** See-through black used by the center menu card. */
	public static final int BOX = 0x80000000;
	public static final int BOX_HOT = 0xA0000000;
	/** 20% dark glass shared by the account pill and the close button. */
	public static final int GLASS_20 = 0x33121216;
	public static final int GLASS_20_HOT = 0x55121216;
	/** Shared top-bar metrics so the account chip and close button line up. */
	public static final int BAR_Y = 6;
	public static final int BAR_H = 20;
	public static final int BAR_GAP = 4;
	public static final int BAR_EDGE = 8;
	private static final int ICON_TEX = 512;

	private static Identifier closeIcon;

	private OpticMenuChrome() {
	}

	public static final class Layout {
		/** Centered column: logo, wordmark, then the buttons. */
		public int cardX;
		public int cardY;
		public int cardW;
		public int cardH;
		public int logoY;
		public int logoSize;
		public int wordY;
		public int btnX;
		public int btnY;
		public int btnW;
		public int btnH;
		public int btnGap;
		public int closeX;
		public int closeY;
		public int closeS;
	}

	public static Layout layout(MinecraftClient client, int screenW, int screenH) {
		Layout layout = new Layout();
		layout.logoSize = screenH < 260 ? 44 : 56;
		layout.btnW = 200;
		layout.btnH = 20;
		layout.btnGap = 5;
		// Logo, "OPTIC" (16px) and the 3 button rows as one tight block, centered on screen.
		int header = layout.logoSize + WORD_GAP + WORD_H + BUTTON_GAP;
		int total = header + 3 * layout.btnH + 2 * layout.btnGap;
		int top = Math.max(BAR_Y + BAR_H + 6, (screenH - total) / 2);
		layout.cardW = layout.btnW;
		layout.cardH = total;
		layout.cardX = (screenW - layout.cardW) / 2;
		layout.cardY = top;
		layout.logoY = top;
		layout.wordY = top + layout.logoSize + WORD_GAP;
		layout.btnX = layout.cardX;
		layout.btnY = top + header;

		layout.closeS = BAR_H;
		layout.closeX = screenW - BAR_EDGE - layout.closeS;
		layout.closeY = BAR_Y;
		return layout;
	}

	/**
	 * Backdrop over the blurred panorama: a light dark wash, a darker band at the bottom
	 * (Lunar-style vignette) and a faint accent tint behind the logo.
	 */
	public static void drawCard(MinecraftClient client, Layout layout) {
		int w = client.currentScreen != null ? client.currentScreen.width : 0;
		int h = client.currentScreen != null ? client.currentScreen.height : 0;
		if (w <= 0 || h <= 0) return;
		// One even wash: the gradient bands and the glow behind the logo showed visible steps.
		DrawableHelper.fill(0, 0, w, h, 0x5A050608);
	}

	/** Space between logo and wordmark, wordmark height, and wordmark to first button. */
	private static final int WORD_GAP = 3;
	private static final int WORD_H = 16;
	private static final int BUTTON_GAP = 10;

	/** Logo with the "OPTIC" wordmark under it, centered above the buttons. */
	public static void drawCenterLogo(MinecraftClient client, Layout layout) {
		int w = client.currentScreen != null ? client.currentScreen.width : layout.cardX * 2 + layout.cardW;
		drawLogo(client, (w - layout.logoSize) / 2, layout.logoY, layout.logoSize);
		String word = "OPTIC";
		float size = 16.0F;
		int ww = OpticUiFont.widthSharp(client, word, OpticTheme.TEXT, size);
		OpticUiFont.drawSharp(client, word, (w - ww) / 2, layout.wordY, OpticTheme.TEXT, size);
	}

	public static void drawTopBar(MinecraftClient client, Layout layout, int mouseX, int mouseY) {
		boolean closeHot = inside(mouseX, mouseY, layout.closeX, layout.closeY, layout.closeS, layout.closeS);
		OpticUiDraw.fillExact(client, layout.closeX, layout.closeY, layout.closeS, layout.closeS, 6, closeHot ? 0xCC3A1216 : GLASS_20);
		if (closeHot) OpticUiDraw.outlineExact(client, layout.closeX, layout.closeY, layout.closeS, layout.closeS, 6, 0xFFFF5C6A);
		int glyph = layout.closeS - 10;
		int pad = (layout.closeS - glyph) / 2;
		drawIcon(client, closeIcon(client), layout.closeX + pad, layout.closeY + pad, glyph, 0xFFFFFFFF);
	}

	public static void drawFooter(MinecraftClient client, int screenW, int screenH, Layout layout, int mouseX, int mouseY) {
		float footer = 8.0F;
		String brand = "OPTIC";
		String version = "  " + OpticClient.VERSION + " \u00b7 1.7.10";
		int y = screenH - OpticUiFont.heightSharp(client, brand, ACCENT, footer) - 5;
		OpticUiFont.drawSharp(client, brand, 6, y, ACCENT, footer);
		int bw = OpticUiFont.widthSharp(client, brand, ACCENT, footer);
		OpticUiFont.drawSharp(client, version, 6 + bw, y, OpticTheme.MUTED, footer);

		String copyright = "Copyright Mojang AB. Do not distribute!";
		int cw = OpticUiFont.widthSharp(client, copyright, OpticTheme.FAINT, footer);
		OpticUiFont.drawSharp(client, copyright, Math.max(6, screenW - cw - 6), y, OpticTheme.FAINT, footer);
	}

	/** accentLine drawn right-to-left, so a centered rule fades out on both sides. */
	private static void mirroredAccentLine(int x, int y, int w) {
		int steps = Math.max(1, Math.min(32, w / 3));
		for (int i = 0; i < steps; i++) {
			int x1 = x + w - w * (i + 1) / steps;
			int x2 = x + w - w * i / steps;
			float t = i / (float) Math.max(1, steps - 1);
			int rgb = OpticTheme.lerp(ACCENT, OpticTheme.ACCENT_2, Math.min(1.0F, t * 1.6F)) & 0xFFFFFF;
			int alpha = Math.round(255 * (t < 0.6F ? 1.0F : 1.0F - (t - 0.6F) / 0.4F));
			DrawableHelper.fill(x1, y, x2, y + 1, (Math.max(0, alpha) << 24) | rgb);
		}
	}

	/** Vertical gradient (top color -> bottom color). */
	private static void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
		int steps = Math.max(1, Math.min(64, (y2 - y1) / 2));
		for (int i = 0; i < steps; i++) {
			int a = y1 + (y2 - y1) * i / steps;
			int b = y1 + (y2 - y1) * (i + 1) / steps;
			DrawableHelper.fill(x1, a, x2, b, OpticTheme.lerp(top, bottom, i / (float) Math.max(1, steps - 1)));
		}
	}
	public static boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	public static void drawLogo(MinecraftClient client, int x, int y, int size) {
		Identifier logo = OpticTextures.logo(client);
		if (logo == null || size <= 0) return;
		gg.optic.client.OpticSmooth.bindMip(client, logo);
		int tw = Math.max(1, OpticTextures.logoWidth());
		int th = Math.max(1, OpticTextures.logoHeight());
		float aspect = tw / (float) th;
		int dw = size;
		int dh = size;
		if (aspect > 1.01F) {
			dh = Math.max(1, Math.round(size / aspect));
		} else if (aspect < 0.99F) {
			dw = Math.max(1, Math.round(size * aspect));
		}
		int dx = x + (size - dw) / 2;
		int dy = y + (size - dh) / 2;
		DrawableHelper.drawTexture(dx, dy, 0.0F, 0.0F, tw, th, dw, dh, (float) tw, (float) th);
	}

	private static void drawIcon(MinecraftClient client, Identifier icon, int x, int y, int size, int argb) {
		if (icon == null || size <= 0) return;
		float a = ((argb >> 24) & 0xFF) / 255.0F;
		float r = ((argb >> 16) & 0xFF) / 255.0F;
		float g = ((argb >> 8) & 0xFF) / 255.0F;
		float b = (argb & 0xFF) / 255.0F;
		gg.optic.client.OpticSmooth.bind(client, icon);
		GlStateManager.color(r, g, b, a);
		DrawableHelper.drawTexture(x, y, 0.0F, 0.0F, ICON_TEX, ICON_TEX, size, size, ICON_TEX, ICON_TEX);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static Identifier closeIcon(MinecraftClient client) {
		if (closeIcon != null) return closeIcon;
		closeIcon = bakeShape(client, "optic_menu_close", 2);
		return closeIcon;
	}

	/** kind: 2 close. */
	private static Identifier bakeShape(MinecraftClient client, String name, int kind) {
		try {
			int size = ICON_TEX;
			BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setColor(Color.WHITE);
			float stroke = size / 22.0F;
			g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			float pad = size * 0.16F;
			if (kind == 2) {
				int a = Math.round(pad);
				int b = size - a;
				g.drawLine(a, a, b, b);
				g.drawLine(b, a, a, b);
			} else {
				float d = size * 0.42F;
				g.draw(new Ellipse2D.Float(pad, pad * 0.7F, d, d));
				g.drawLine(Math.round(pad + d * 0.72F), Math.round(pad * 0.7F + d * 0.72F), Math.round(size - pad), Math.round(size - pad));
			}
			g.dispose();
			NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
			Identifier id = client.getTextureManager().registerDynamicTexture(name, texture);
			gg.optic.client.OpticSmooth.linearize(texture);
			return id;
		} catch (Exception e) {
			System.err.println("[Optic] Menu icon failed: " + e.getMessage());
			return null;
		}
	}
}
