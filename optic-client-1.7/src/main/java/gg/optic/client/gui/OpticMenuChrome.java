package gg.optic.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticClient;
import gg.optic.client.OpticTextures;
import gg.optic.client.OpticUiDraw;
import gg.optic.client.OpticUiFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.Tessellator;
import org.lwjgl.opengl.GL11;
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
		/** Portal-style: wide logo top-left, a left-aligned list of text items in the lower left. */
		public int logoX;
		public int logoY;
		public int logoW;
		public int logoH;
		public int btnX;
		public int btnY;
		public int btnW;
		public int btnH;
		public int btnGap;
		public int closeX;
		public int closeY;
		public int closeS;
	}

	public static final String MENU_LOGO = "menu_logo.png";
	public static final String MENU_BACKGROUND = "menu_background.png";
	/** Number of list items: Singleplayer, Multiplayer, Mods, Options, Quit. */
	public static final int ITEMS = 5;
	/** Text inset from the start of the hover bar. */
	public static final int ITEM_PAD = 8;

	public static Layout layout(MinecraftClient client, int screenW, int screenH) {
		Layout layout = new Layout();
		int left = Math.max(18, Math.round(screenW * 0.07F));
		layout.logoW = Math.max(150, Math.min(300, Math.round(screenW * 0.36F)));
		layout.logoH = Math.round(layout.logoW * (274.0F / 1024.0F));
		layout.logoX = left;
		layout.logoY = Math.max(BAR_Y + BAR_H + 14, Math.round(screenH * 0.15F));

		layout.btnH = 16;
		layout.btnGap = 2;
		layout.btnW = 190;
		layout.btnX = left - ITEM_PAD;
		// The list sits right under the logo.
		layout.btnY = layout.logoY + layout.logoH + 12;

		layout.closeS = BAR_H;
		layout.closeX = screenW - BAR_EDGE - layout.closeS;
		layout.closeY = BAR_Y;
		return layout;
	}

	/** Wallpaper, scaled to cover the screen, with a dark fade on the left so text and logo read. */
	public static void drawCard(MinecraftClient client, Layout layout) {
		int w = client.currentScreen != null ? client.currentScreen.width : 0;
		int h = client.currentScreen != null ? client.currentScreen.height : 0;
		if (w <= 0 || h <= 0) return;
		drawWallpaper(client, w, h);
		DrawableHelper.fill(0, 0, w, h, 0x26000000);
		horizontalFade(0, 0, Math.round(w * 0.62F), h, 0x8C000000);
	}

	/** The menu wallpaper, scaled to cover the whole screen without stretching. */
	public static void drawWallpaper(MinecraftClient client, int w, int h) {
		Identifier bg = OpticTextures.image(client, MENU_BACKGROUND);
		if (bg != null) {
			int[] size = OpticTextures.imageSize(MENU_BACKGROUND);
			float scale = Math.max(w / (float) size[0], h / (float) size[1]);
			int dw = (int) Math.ceil(size[0] * scale);
			int dh = (int) Math.ceil(size[1] * scale);
			GlStateManager.enableTexture();
			GlStateManager.disableBlend();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			client.getTextureManager().bindTexture(bg);
			DrawableHelper.drawTexture((w - dw) / 2, (h - dh) / 2, 0.0F, 0.0F, size[0], size[1], dw, dh, (float) size[0], (float) size[1]);
		} else {
			DrawableHelper.fill(0, 0, w, h, 0xFF2A2C30);
		}
	}

	/**
	 * Left-to-right fade from {@code argb} to fully clear. Drawn as a few quads with per-vertex
	 * alpha and smooth shading, so the GPU interpolates it: no stepped strips, no visible lines.
	 */
	private static void horizontalFade(int x, int y, int w, int h, int argb) {
		float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
		float r = ((argb >> 16) & 0xFF) / 255.0F;
		float g = ((argb >> 8) & 0xFF) / 255.0F;
		float b = (argb & 0xFF) / 255.0F;
		int segments = 12;
		GlStateManager.disableTexture();
		GlStateManager.enableBlend();
		GlStateManager.disableAlphaTest();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		Tessellator buf = Tessellator.INSTANCE;
		buf.begin(7);
		for (int i = 0; i < segments; i++) {
			float t0 = i / (float) segments;
			float t1 = (i + 1) / (float) segments;
			double x0 = x + w * t0;
			double x1 = x + w * t1;
			// Smoothstep falloff: flat start, soft tail, no kink where the fade ends.
			float a0 = alpha * (1.0F - smooth(t0));
			float a1 = alpha * (1.0F - smooth(t1));
			buf.color(r, g, b, a1);
			buf.vertex(x1, y, 0.0);
			buf.color(r, g, b, a0);
			buf.vertex(x0, y, 0.0);
			buf.vertex(x0, y + h, 0.0);
			buf.color(r, g, b, a1);
			buf.vertex(x1, y + h, 0.0);
		}
		buf.end();
		GL11.glShadeModel(GL11.GL_FLAT);
		GlStateManager.enableAlphaTest();
		GlStateManager.enableTexture();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static float smooth(float t) {
		return t * t * (3.0F - 2.0F * t);
	}
	/** The Optic wordmark at the top-left, with a soft drop shadow against the light sky. */
	public static void drawCenterLogo(MinecraftClient client, Layout layout) {
		Identifier logo = OpticTextures.image(client, MENU_LOGO);
		if (logo == null) return;
		int[] size = OpticTextures.imageSize(MENU_LOGO);
		GlStateManager.enableTexture();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		client.getTextureManager().bindTexture(logo);
		GlStateManager.color(0.0F, 0.0F, 0.0F, 0.35F);
		DrawableHelper.drawTexture(layout.logoX + 1, layout.logoY + 2, 0.0F, 0.0F, size[0], size[1], layout.logoW, layout.logoH, (float) size[0], (float) size[1]);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		DrawableHelper.drawTexture(layout.logoX, layout.logoY, 0.0F, 0.0F, size[0], size[1], layout.logoW, layout.logoH, (float) size[0], (float) size[1]);
	}

	/** One list item: plain uppercase text, a translucent bar behind it on hover. */
	public static void drawMenuItem(MinecraftClient client, int x, int y, int w, int h, String label, boolean hot, boolean active) {
		if (hot) {
			int solid = Math.round(w * 0.7F);
			DrawableHelper.fill(x, y, x + solid, y + h, 0x38FFFFFF);
			int steps = 24;
			for (int i = 0; i < steps; i++) {
				int x1 = x + solid + (w - solid) * i / steps;
				int x2 = x + solid + (w - solid) * (i + 1) / steps;
				int a = Math.round(0x38 * (1.0F - i / (float) steps));
				DrawableHelper.fill(x1, y, x2, y + h, (a << 24) | 0xFFFFFF);
			}
			DrawableHelper.fill(x, y, x + 2, y + h, ACCENT);
		}
		float size = 10.0F;
		int color = !active ? 0xFF8A8A8A : hot ? 0xFFFFFFFF : 0xFFE4E4E4;
		int th = OpticUiFont.heightSharp(client, label, color, size);
		int tx = x + ITEM_PAD;
		int ty = y + (h - th) / 2;
		OpticUiFont.drawSharp(client, label, tx + 1, ty + 1, 0x8C000000, size);
		OpticUiFont.drawSharp(client, label, tx, ty, color, size);
	}
	/** Startup fade: the last loading-screen frame dissolves into the menu, once per launch. */
	private static long introStart = -1L;
	private static boolean introDone;
	private static final long INTRO_HOLD_MS = 150L;
	private static final long INTRO_FADE_MS = 700L;
	private static java.awt.image.BufferedImage splashPixels;
	private static Identifier splashId;
	private static int splashW = 1;
	private static int splashH = 1;

	/** Reads the splash frame from the back buffer (called right before it is presented). */
	public static void captureSplash() {
		try {
			int w = org.lwjgl.opengl.Display.getWidth();
			int h = org.lwjgl.opengl.Display.getHeight();
			if (w <= 0 || h <= 0) return;
			java.nio.ByteBuffer buf = org.lwjgl.BufferUtils.createByteBuffer(w * h * 4);
			GL11.glReadBuffer(GL11.GL_BACK);
			GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
			GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
			BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < h; y++) {
				int row = (h - 1 - y) * w * 4;
				for (int x = 0; x < w; x++) {
					int i = row + x * 4;
					int r = buf.get(i) & 0xFF;
					int g = buf.get(i + 1) & 0xFF;
					int b = buf.get(i + 2) & 0xFF;
					image.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
				}
			}
			splashPixels = image;
			splashW = w;
			splashH = h;
		} catch (Throwable e) {
			System.err.println("[Optic] Splash capture failed: " + e);
		}
	}

	public static void drawIntro(MinecraftClient client, int w, int h) {
		if (introDone || w <= 0 || h <= 0) return;
		long now = System.currentTimeMillis();
		if (introStart < 0L) introStart = now;
		long t = now - introStart - INTRO_HOLD_MS;
		float k = t <= 0L ? 0.0F : Math.min(1.0F, t / (float) INTRO_FADE_MS);
		if (k >= 1.0F) {
			introDone = true;
			splashPixels = null;
			if (splashId != null) client.getTextureManager().close(splashId);
			splashId = null;
			return;
		}
		float alpha = 1.0F - smooth(k);
		if (splashId == null && splashPixels != null) {
			try {
				NativeImageBackedTexture texture = new NativeImageBackedTexture(splashPixels);
				splashId = client.getTextureManager().registerDynamicTexture("optic_splash", texture);
				gg.optic.client.OpticSmooth.linearize(texture);
			} catch (Throwable e) {
				System.err.println("[Optic] Splash texture failed: " + e);
			}
			splashPixels = null;
		}
		if (splashId != null) {
			int tw = splashW;
			int th = splashH;
			GlStateManager.enableTexture();
			GlStateManager.enableBlend();
			GlStateManager.blendFuncSeparate(770, 771, 1, 0);
			GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
			client.getTextureManager().bindTexture(splashId);
			// The whole captured frame stretched over the screen: same picture, just fading out.
			DrawableHelper.drawTexture(0, 0, 0.0F, 0.0F, tw, th, w, h, (float) tw, (float) th);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		} else {
			DrawableHelper.fill(0, 0, w, h, (Math.round(alpha * 255.0F) << 24) | 0xFFFFFF);
		}
	}
	private static final String COSMETICS = "COSMETICS";
	private static final float COSMETICS_SIZE = 8.0F;

	/** Hit box of the Cosmetics button: top-right, level with the account chip. {x, y, w, h} */
	public static int[] cosmeticsBounds(MinecraftClient client, int screenW) {
		int w = OpticUiFont.widthSharp(client, COSMETICS, 0xFFFFFFFF, COSMETICS_SIZE) + 8;
		return new int[] { screenW - BAR_EDGE - w, BAR_Y, w, BAR_H };
	}

	/** Same look as the account chip: no background, text turns accent on hover. */
	public static void drawCosmetics(MinecraftClient client, int screenW, int mouseX, int mouseY) {
		int[] b = cosmeticsBounds(client, screenW);
		boolean hot = inside(mouseX, mouseY, b[0], b[1], b[2], b[3]);
		int color = hot ? ACCENT : OpticTheme.TEXT;
		int th = OpticUiFont.heightSharp(client, COSMETICS, color, COSMETICS_SIZE);
		OpticUiFont.drawSharp(client, COSMETICS, b[0] + 4, b[1] + (b[3] - th) / 2, color, COSMETICS_SIZE);
	}

	/** @return true if the click hit the Cosmetics button */
	public static boolean clickCosmetics(MinecraftClient client, int screenW, int mouseX, int mouseY) {
		int[] b = cosmeticsBounds(client, screenW);
		if (!inside(mouseX, mouseY, b[0], b[1], b[2], b[3])) return false;
		client.setScreen(new OpticCosmeticsScreen(client.currentScreen));
		return true;
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
		// Bottom left: the game version, in the same colour as the Mojang line on the right.
		String version = "Minecraft 1.7.10";
		int y = screenH - OpticUiFont.heightSharp(client, version, OpticTheme.FAINT, footer) - 5;
		OpticUiFont.drawSharp(client, version, 6, y, OpticTheme.FAINT, footer);

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
