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
	private static final int ACCENT = 0xFF5CFF8A;
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
		public int cardX;
		public int cardY;
		public int cardW;
		public int cardH;
		public int btnX;
		public int btnY;
		public int btnW;
		public int btnH;
		public int closeX;
		public int closeY;
		public int closeS;
	}

	public static Layout layout(MinecraftClient client, int screenW, int screenH) {
		Layout layout = new Layout();
		layout.cardW = 148;
		layout.cardH = 128;
		layout.cardX = (screenW - layout.cardW) / 2;
		layout.cardY = (screenH - layout.cardH) / 2 - 8;
		layout.btnW = 116;
		layout.btnH = 16;
		layout.btnX = layout.cardX + (layout.cardW - layout.btnW) / 2;
		layout.btnY = layout.cardY + 76;

		layout.closeS = BAR_H;
		layout.closeX = screenW - BAR_EDGE - layout.closeS;
		layout.closeY = BAR_Y;
		return layout;
	}

	public static void drawCard(MinecraftClient client, Layout layout) {
		OpticUiDraw.fillExact(client, layout.cardX, layout.cardY, layout.cardW, layout.cardH, 10, BOX);
	}

	public static void drawCenterLogo(MinecraftClient client, Layout layout) {
		int size = 52;
		int headerH = Math.max(size, layout.btnY - layout.cardY);
		int x = layout.cardX + (layout.cardW - size) / 2;
		int y = layout.cardY + (headerH - size) / 2;
		drawLogo(client, x, y, size);
	}

	public static void drawTopBar(MinecraftClient client, Layout layout, int mouseX, int mouseY) {
		boolean closeHot = inside(mouseX, mouseY, layout.closeX, layout.closeY, layout.closeS, layout.closeS);
		OpticUiDraw.fillExact(client, layout.closeX, layout.closeY, layout.closeS, layout.closeS, 6, closeHot ? GLASS_20_HOT : GLASS_20);
		int glyph = layout.closeS - 10;
		int pad = (layout.closeS - glyph) / 2;
		drawIcon(client, closeIcon(client), layout.closeX + pad, layout.closeY + pad, glyph, 0xFFFFFFFF);
	}

	public static void drawFooter(MinecraftClient client, int screenW, int screenH, Layout layout, int mouseX, int mouseY) {
		String version = "Optic (" + OpticClient.VERSION + ")";
		float footer = 9.0F;
		int y = screenH - OpticUiFont.heightSharp(client, version, ACCENT, footer) - 4;
		OpticUiFont.drawSharp(client, version, 4, y, ACCENT, footer);

		String copyright = "Copyright Mojang AB. Do not distribute!";
		int cw = OpticUiFont.widthSharp(client, copyright, 0xFFE4E4EA, footer);
		OpticUiFont.drawSharp(client, copyright, Math.max(4, screenW - cw - 4), y, 0xFFE4E4EA, footer);
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
