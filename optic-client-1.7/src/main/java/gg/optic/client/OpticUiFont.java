package gg.optic.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Smooth UI text. Glyphs are rasterized well above the window resolution
 * and drawn with linear filtering so edges stay sharp instead of chunky.
 */
public final class OpticUiFont {
	private static final float GUI_SIZE = 9.0F;
	private static final AffineTransform IDENTITY = new AffineTransform();
	private static final Map<String, Label> CACHE = new HashMap<String, Label>();

	private static Font root;
	private static Font emojiRoot;
	private static int bakedScale;
	private static Label brand;
	private static Label copyright;

	private OpticUiFont() {
	}

	public static void draw(MinecraftClient client, String text, int x, int y, int argb) {
		drawSized(client, text, x, y, argb, GUI_SIZE);
	}

	public static void drawSized(MinecraftClient client, String text, int x, int y, int argb, float guiSize) {
		Label label = label(client, text, argb, guiSize);
		if (label != null) label.draw(client, x, y);
	}

	public static void drawCenteredSized(MinecraftClient client, String text, int centerX, int y, int argb, float guiSize) {
		Label label = label(client, text, argb, guiSize);
		if (label == null) return;
		label.draw(client, centerX - label.drawW / 2, y);
	}

	/** Hi-res emoji / symbol glyphs (uses a system emoji font, not Montserrat). */
	public static void drawEmojiCentered(MinecraftClient client, String emoji, int centerX, int y, float guiSize) {
		Label label = emojiLabel(client, emoji, guiSize);
		if (label == null) return;
		label.draw(client, centerX - label.drawW / 2, y);
	}

	public static int emojiWidth(MinecraftClient client, String emoji, float guiSize) {
		Label label = emojiLabel(client, emoji, guiSize);
		return label == null ? Math.round(guiSize) : label.drawW;
	}

	public static int emojiHeight(MinecraftClient client, String emoji, float guiSize) {
		Label label = emojiLabel(client, emoji, guiSize);
		return label == null ? Math.round(guiSize) : label.drawH;
	}

	public static int width(MinecraftClient client, String text, int argb) {
		return widthSized(client, text, argb, GUI_SIZE);
	}

	public static int widthSized(MinecraftClient client, String text, int argb, float guiSize) {
		Label label = label(client, text, argb, guiSize);
		return label == null ? 0 : label.drawW;
	}

	public static int heightSized(MinecraftClient client, String text, int argb, float guiSize) {
		Label label = label(client, text, argb, guiSize);
		return label == null ? Math.round(guiSize) : label.drawH;
	}

	/** Menu text: one string, tight padding, light supersample so it stays sharp. */
	/** Crisp label, centered on both axes. One texel per screen pixel so the letters stay sharp. */
	public static void drawCrispCentered(MinecraftClient client, String text, int centerX, int centerY, int argb, float guiSize) {
		Label label = crispLabel(client, text, argb, guiSize);
		if (label == null) return;
		label.draw(client, centerX - label.drawW / 2, centerY - label.drawH / 2);
	}

	public static void drawSharp(MinecraftClient client, String text, int x, int y, int argb, float guiSize) {
		Label label = sharpLabel(client, text, argb, guiSize);
		if (label != null) label.draw(client, x, y);
	}

	public static int widthSharp(MinecraftClient client, String text, int argb, float guiSize) {
		Label label = sharpLabel(client, text, argb, guiSize);
		return label == null ? 0 : label.drawW;
	}

	public static int heightSharp(MinecraftClient client, String text, int argb, float guiSize) {
		Label label = sharpLabel(client, text, argb, guiSize);
		return label == null ? Math.round(guiSize) : label.drawH;
	}

	private static Label crispLabel(MinecraftClient client, String text, int argb, float guiSize) {
		if (text == null || text.isEmpty()) return null;
		ensureScale(client);
		String key = "crisp|" + bakedScale + "|" + guiSize + "|" + Integer.toHexString(argb) + "|" + text;
		Label cached = CACHE.get(key);
		if (cached != null) return cached;
		cached = bakeCrisp(client, text, new Color(argb, true), bakedScale, guiSize, "optic_crisp_" + CACHE.size());
		if (cached != null) CACHE.put(key, cached);
		return cached;
	}

	private static Label bakeCrisp(MinecraftClient client, String text, Color color, int sf, float guiSize, String name) {
		return rasterCrisp(client, text, color, sf, guiSize, Font.PLAIN, name);
	}

	/**
	 * Rasterize at the window scale so each font texel lands on one screen pixel.
	 * Downscaling a huge texture was what made the letters look soft.
	 */
	private static Label rasterCrisp(MinecraftClient client, String text, Color color, int sf, float guiSize, int style, String name) {
		try {
			int ss = Math.max(2, sf);
			float px = guiSize * ss;
			Font font = root().deriveFont(style, px);
			FontRenderContext frc = new FontRenderContext(IDENTITY, true, false);
			TextLayout layout = new TextLayout(text, font, frc);

			int pad = Math.max(1, ss / 6);
			int texW = Math.max(1, (int) Math.ceil(layout.getAdvance()) + pad * 2);
			int texH = Math.max(1, (int) Math.ceil(layout.getAscent() + layout.getDescent()) + pad * 2);
			texW = ((texW + ss - 1) / ss) * ss;
			texH = ((texH + ss - 1) / ss) * ss;

			BufferedImage image = new BufferedImage(texW, texH, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setTransform(IDENTITY);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(color);
			float baseline = (texH - (layout.getAscent() + layout.getDescent())) * 0.5F + layout.getAscent();
			float left = (texW - layout.getAdvance()) * 0.5F;
			layout.draw(g, left, baseline);
			g.dispose();

			Identifier id = upload(client, image, name);
			return new Label(id, texW, texH, Math.max(1, texW / ss), Math.max(1, texH / ss), true);
		} catch (Exception e) {
			System.err.println("[Optic] Text bake failed: " + e.getMessage());
			return null;
		}
	}

	private static Label sharpLabel(MinecraftClient client, String text, int argb, float guiSize) {
		if (text == null || text.isEmpty()) return null;
		ensureScale(client);
		String key = "sharp|" + bakedScale + "|" + guiSize + "|" + Integer.toHexString(argb) + "|" + text;
		Label cached = CACHE.get(key);
		if (cached != null) return cached;
		cached = bakeSharp(client, text, new Color(argb, true), bakedScale, guiSize, "optic_sharp_" + CACHE.size());
		if (cached != null) CACHE.put(key, cached);
		return cached;
	}

	private static Label label(MinecraftClient client, String text, int argb, float guiSize) {
		if (text == null || text.isEmpty()) return null;
		ensureScale(client);
		String key = bakedScale + "|" + guiSize + "|" + Integer.toHexString(argb) + "|" + text;
		Label cached = CACHE.get(key);
		if (cached != null) return cached;
		int style = guiSize >= 18.0F ? Font.BOLD : Font.PLAIN;
		cached = bake(client, text, new Color(argb, true), bakedScale, guiSize, style, "optic_ui_" + CACHE.size());
		if (cached != null) CACHE.put(key, cached);
		return cached;
	}

	public static void drawBrand(MinecraftClient client, int x, int y) {
		ensure(client);
		if (brand != null) brand.draw(client, x, y);
	}

	public static void drawCopyright(MinecraftClient client, int x, int y) {
		ensure(client);
		if (copyright != null) copyright.draw(client, x, y);
	}

	public static int copyrightWidth(MinecraftClient client) {
		ensure(client);
		return copyright == null ? 0 : copyright.drawW;
	}

	public static int lineHeight(MinecraftClient client) {
		ensure(client);
		return brand == null ? 10 : brand.drawH;
	}

	private static void ensureScale(MinecraftClient client) {
		int sf = Math.max(1, new Window(client, client.width, client.height).getScaleFactor());
		if (sf != bakedScale) {
			CACHE.clear();
			brand = null;
			copyright = null;
			bakedScale = sf;
		}
	}

	private static void ensure(MinecraftClient client) {
		ensureScale(client);
		if (brand != null && copyright != null) return;
		brand = bake(client, "Optic 1.8.9 (" + OpticClient.VERSION + ")", new Color(0xFF66CC33, true), bakedScale, GUI_SIZE, Font.PLAIN, "optic_footer_brand");
		copyright = bake(client, "Copyright Mojang AB. Do not distribute!", Color.WHITE, bakedScale, GUI_SIZE, Font.PLAIN, "optic_footer_copyright");
	}

	private static Font root() {
		if (root != null) return root;
		try (InputStream in = OpticUiFont.class.getResourceAsStream("/assets/optic/fonts/montserrat.ttf")) {
			if (in != null) {
				root = Font.createFont(Font.TRUETYPE_FONT, in);
				return root;
			}
		} catch (Exception e) {
			System.err.println("[Optic] Font load failed: " + e.getMessage());
		}
		root = new Font("SansSerif", Font.PLAIN, 1);
		return root;
	}

	private static Font emojiRoot() {
		if (emojiRoot != null) return emojiRoot;
		String[] families = {
			"Segoe UI Emoji",
			"Segoe UI Symbol",
			"Apple Color Emoji",
			"Noto Color Emoji",
			"Twitter Color Emoji",
			"SansSerif"
		};
		for (int i = 0; i < families.length; i++) {
			Font candidate = new Font(families[i], Font.PLAIN, 1);
			// Prefer a font that can draw common UI emoji.
			if (candidate.canDisplay(0x1F4DD) || candidate.canDisplay(0x270F) || candidate.canDisplay(0x1F5D1)) {
				emojiRoot = candidate;
				return emojiRoot;
			}
		}
		emojiRoot = new Font("SansSerif", Font.PLAIN, 1);
		return emojiRoot;
	}

	private static Label emojiLabel(MinecraftClient client, String emoji, float guiSize) {
		if (emoji == null || emoji.isEmpty()) return null;
		ensureScale(client);
		String key = "emoji|" + bakedScale + "|" + guiSize + "|" + emoji;
		Label cached = CACHE.get(key);
		if (cached != null) return cached;
		cached = bakeEmoji(client, emoji, bakedScale, guiSize, "optic_emoji_" + CACHE.size());
		if (cached != null) CACHE.put(key, cached);
		return cached;
	}

	private static Label bake(MinecraftClient client, String text, Color color, int sf, float guiSize, int style, String name) {
		return rasterCrisp(client, text, color, sf, guiSize, style, name);
	}

	private static Label bakeSharp(MinecraftClient client, String text, Color color, int sf, float guiSize, String name) {
		return rasterCrisp(client, text, color, sf, guiSize, Font.PLAIN, name);
	}

	private static Label bakeEmoji(MinecraftClient client, String emoji, int sf, float guiSize, String name) {
		try {
			// Supersample well above GUI pixels (never stick to ~32px — looks muddy when scaled).
			int draw = Math.max(1, Math.round(guiSize));
			int bakeSf = Math.max(8, sf * 4);
			int cell = Math.max(128, draw * bakeSf);
			float px = cell * 0.72F;
			Font font = emojiRoot().deriveFont(Font.PLAIN, px);
			FontRenderContext frc = new FontRenderContext(IDENTITY, true, true);
			TextLayout layout = new TextLayout(emoji, font, frc);

			BufferedImage image = new BufferedImage(cell, cell, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setTransform(IDENTITY);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setClip(0, 0, cell, cell);

			float glyphW = layout.getAdvance();
			float glyphH = layout.getAscent() + layout.getDescent();
			float drawX = (cell - glyphW) * 0.5F;
			float baseline = (cell - glyphH) * 0.5F + layout.getAscent();
			g.setColor(Color.WHITE);
			layout.draw(g, drawX, baseline);
			g.dispose();

			Identifier id = upload(client, image, name);

			return new Label(id, cell, cell, draw, draw);
		} catch (Exception e) {
			System.err.println("[Optic] Emoji bake failed: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/** Two crossed swords, centered the same way as the favorite star. */
	public static void drawSwordsCentered(MinecraftClient client, int centerX, int centerY, float guiSize, int argb) {
		Label label = swordsLabel(client, guiSize, argb);
		if (label == null) return;
		label.draw(client, centerX - label.drawW / 2, centerY - label.drawH / 2);
	}

	/** Hi-res favorite star (outline or filled), baked ≥128px then scaled down. */
	public static void drawStarCentered(MinecraftClient client, int centerX, int centerY, float guiSize, int argb, boolean filled) {
		Label label = starLabel(client, guiSize, argb, filled);
		if (label == null) return;
		label.draw(client, centerX - label.drawW / 2, centerY - label.drawH / 2);
	}

	private static Label swordsLabel(MinecraftClient client, float guiSize, int argb) {
		ensureScale(client);
		String key = "swords|" + bakedScale + "|" + guiSize + "|" + Integer.toHexString(argb);
		Label cached = CACHE.get(key);
		if (cached != null) return cached;
		cached = bakeSwords(client, bakedScale, guiSize, argb, "optic_swords_" + CACHE.size());
		if (cached != null) CACHE.put(key, cached);
		return cached;
	}

	private static Label bakeSwords(MinecraftClient client, int sf, float guiSize, int argb, String name) {
		try {
			int draw = Math.max(8, Math.round(guiSize));
			int bakeSf = Math.max(8, sf * 4);
			int cell = Math.max(128, draw * bakeSf);
			BufferedImage image = new BufferedImage(cell, cell, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setTransform(IDENTITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			Color color = new Color(argb, true);
			g.setColor(color);
			float pad = cell * 0.13F;
			float thick = cell * 0.085F;
			drawSword(g, pad, cell - pad, cell - pad, pad, thick);
			drawSword(g, cell - pad, cell - pad, pad, pad, thick);
			g.dispose();
			Identifier id = upload(client, image, name);
			return new Label(id, cell, cell, draw, draw);
		} catch (Exception e) {
			System.err.println("[Optic] Swords bake failed: " + e.getMessage());
			return null;
		}
	}

	/**
	 * One sword, handle end at (x0, y0) and tip at (x1, y1): round pommel, grip, crossguard,
	 * then a straight blade with parallel edges that tapers to a point.
	 */
	private static void drawSword(Graphics2D g, float x0, float y0, float x1, float y1, float thick) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float len = (float) Math.sqrt(dx * dx + dy * dy);
		if (len < 1.0F) return;
		float ux = dx / len;
		float uy = dy / len;
		float px = -uy;
		float py = ux;

		// Pommel.
		float pr = thick * 0.55F;
		g.fill(new java.awt.geom.Ellipse2D.Float(x0 - pr, y0 - pr, pr * 2.0F, pr * 2.0F));

		// Grip.
		float gx = x0 + ux * len * 0.27F;
		float gy = y0 + uy * len * 0.27F;
		g.setStroke(new java.awt.BasicStroke(thick * 0.55F, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_ROUND));
		g.draw(new java.awt.geom.Line2D.Float(x0, y0, gx, gy));

		// Crossguard.
		float guard = thick * 1.9F;
		g.setStroke(new java.awt.BasicStroke(thick * 0.6F, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
		g.draw(new java.awt.geom.Line2D.Float(gx - px * guard, gy - py * guard, gx + px * guard, gy + py * guard));

		// Blade: straight edges to 84% of the length, then a point.
		float half = thick * 0.4F;
		float bx = x0 + ux * len * 0.30F;
		float by = y0 + uy * len * 0.30F;
		float sx = x0 + ux * len * 0.84F;
		float sy = y0 + uy * len * 0.84F;
		java.awt.geom.Path2D.Float blade = new java.awt.geom.Path2D.Float();
		blade.moveTo(bx + px * half, by + py * half);
		blade.lineTo(sx + px * half, sy + py * half);
		blade.lineTo(x1, y1);
		blade.lineTo(sx - px * half, sy - py * half);
		blade.lineTo(bx - px * half, by - py * half);
		blade.closePath();
		g.fill(blade);
	}

	/** "All" icon: a 2x2 grid of rounded tiles, centered like the star and swords. */
	public static void drawGridCentered(MinecraftClient client, int centerX, int centerY, float guiSize, int argb) {
		ensureScale(client);
		String key = "grid|" + bakedScale + "|" + guiSize + "|" + Integer.toHexString(argb);
		Label label = CACHE.get(key);
		if (label == null) {
			label = bakeGrid(client, bakedScale, guiSize, argb, "optic_grid_" + CACHE.size());
			if (label != null) CACHE.put(key, label);
		}
		if (label == null) return;
		label.draw(client, centerX - label.drawW / 2, centerY - label.drawH / 2);
	}

	private static Label bakeGrid(MinecraftClient client, int sf, float guiSize, int argb, String name) {
		try {
			int draw = Math.max(8, Math.round(guiSize));
			int bakeSf = Math.max(8, sf * 4);
			int cell = Math.max(128, draw * bakeSf);
			BufferedImage image = new BufferedImage(cell, cell, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setTransform(IDENTITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setColor(new Color(argb, true));
			float pad = cell * 0.16F;
			float gap = cell * 0.1F;
			float tile = (cell - pad * 2.0F - gap) / 2.0F;
			float arc = tile * 0.45F;
			for (int r = 0; r < 2; r++) {
				for (int c = 0; c < 2; c++) {
					float tx = pad + c * (tile + gap);
					float ty = pad + r * (tile + gap);
					g.fill(new java.awt.geom.RoundRectangle2D.Float(tx, ty, tile, tile, arc, arc));
				}
			}
			g.dispose();
			Identifier id = upload(client, image, name);
			return new Label(id, cell, cell, draw, draw);
		} catch (Exception e) {
			System.err.println("[Optic] Grid icon bake failed: " + e.getMessage());
			return null;
		}
	}

	private static Label starLabel(MinecraftClient client, float guiSize, int argb, boolean filled) {
		ensureScale(client);
		String key = "star|" + bakedScale + "|" + guiSize + "|" + Integer.toHexString(argb) + "|" + filled;
		Label cached = CACHE.get(key);
		if (cached != null) return cached;
		cached = bakeStar(client, bakedScale, guiSize, argb, filled, "optic_star_" + CACHE.size());
		if (cached != null) CACHE.put(key, cached);
		return cached;
	}

	private static Label bakeStar(MinecraftClient client, int sf, float guiSize, int argb, boolean filled, String name) {
		try {
			int draw = Math.max(8, Math.round(guiSize));
			int bakeSf = Math.max(8, sf * 4);
			int cell = Math.max(128, draw * bakeSf);
			BufferedImage image = new BufferedImage(cell, cell, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setTransform(IDENTITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			float cx = cell * 0.5F;
			float cy = cell * 0.5F;
			float outer = cell * 0.42F;
			float inner = outer * 0.42F;
			java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();
			for (int i = 0; i < 10; i++) {
				double ang = -Math.PI / 2.0 + i * Math.PI / 5.0;
				float rad = (i % 2 == 0) ? outer : inner;
				float px = cx + (float) (Math.cos(ang) * rad);
				float py = cy + (float) (Math.sin(ang) * rad);
				if (i == 0) path.moveTo(px, py);
				else path.lineTo(px, py);
			}
			path.closePath();

			Color color = new Color(argb, true);
			if (filled) {
				g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, color.getAlpha() / 3)));
				g.fill(path);
				g.setColor(color);
				g.fill(path);
				g.setStroke(new java.awt.BasicStroke(Math.max(1.5F, cell / 48.0F), java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
				g.setColor(new Color(255, 255, 255, 180));
				g.draw(path);
			} else {
				float stroke = Math.max(2.5F, cell / 28.0F);
				g.setStroke(new java.awt.BasicStroke(stroke + cell / 40.0F, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
				g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, color.getAlpha() / 3)));
				g.draw(path);
				g.setStroke(new java.awt.BasicStroke(stroke, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
				g.setColor(color);
				g.draw(path);
			}
			g.dispose();

			Identifier id = upload(client, image, name);
			return new Label(id, cell, cell, draw, draw);
		} catch (Exception e) {
			System.err.println("[Optic] Star bake failed: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	private static Identifier upload(MinecraftClient client, BufferedImage image, String name) {
		NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
		Identifier id = client.getTextureManager().registerDynamicTexture(name, texture);
		OpticSmooth.linearize(texture);
		return id;
	}

	private static final class Label {
		final Identifier id;
		final int texW;
		final int texH;
		final int drawW;
		final int drawH;
		final boolean crisp;

		Label(Identifier id, int texW, int texH, int drawW, int drawH) {
			this(id, texW, texH, drawW, drawH, false);
		}

		Label(Identifier id, int texW, int texH, int drawW, int drawH, boolean crisp) {
			this.id = id;
			this.texW = texW;
			this.texH = texH;
			this.drawW = drawW;
			this.drawH = drawH;
			this.crisp = crisp;
		}

		void draw(MinecraftClient client, int x, int y) {
			if (crisp) OpticSmooth.bindNearest(client, id);
			else OpticSmooth.bind(client, id);
			// UV 0–1 over the full hi-res atlas, scaled into drawW×drawH GUI pixels.
			DrawableHelper.drawTexture(x, y, 0.0F, 0.0F, drawW, drawH, (float) drawW, (float) drawH);
		}
	}
}
