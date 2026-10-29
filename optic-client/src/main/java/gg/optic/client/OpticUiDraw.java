package gg.optic.client;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * High-res antialiased GUI primitives. Shapes are baked well above GUI resolution
 * and drawn with bilinear filtering so edges stay smooth at any size.
 */
public final class OpticUiDraw {
	private static final int SUPER = 16;
	private static final int MAX_CACHE = 96;
	private static final int MAX_PLATES = 48;
	private static final Map<String, Plate> PLATES = new LinkedHashMap<String, Plate>(32, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Plate> eldest) {
			return size() > MAX_PLATES;
		}
	};
	private static final Map<String, Slice> CACHE = new LinkedHashMap<String, Slice>(64, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Slice> eldest) {
			return size() > MAX_CACHE;
		}
	};
	private static int bakedScale = -1;

	private OpticUiDraw() {
	}

	public static void fillRound(MinecraftClient client, int x, int y, int w, int h, int radius, int argb) {
		if (client == null || w <= 0 || h <= 0) return;
		if (((argb >> 24) & 0xFF) == 0) return;
		int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));
		if (r <= 0) {
			DrawableHelper.fill(x, y, x + w, y + h, argb);
			return;
		}
		Slice slice = slice(client, r, true, 0.0F);
		if (slice == null) {
			DrawableHelper.fill(x, y, x + w, y + h, argb);
			return;
		}
		GlStateManager.enableBlend();
		drawNine(client, slice, x, y, w, h, r, argb);
	}

	/**
	 * Exact rounded rect, rasterized above the window resolution and drawn at the
	 * same GUI size. Used for the main menu and account manager so curves stay smooth.
	 */
	public static void fillExact(MinecraftClient client, int x, int y, int w, int h, int radius, int argb) {
		if (client == null || w <= 0 || h <= 0) return;
		if (((argb >> 24) & 0xFF) == 0) return;
		Plate plate = plate(client, w, h, radius, true, 0.0F);
		if (plate == null) {
			fillRound(client, x, y, w, h, radius, argb);
			return;
		}
		drawPlate(client, plate, x, y, w, h, argb);
	}

	public static void outlineExact(MinecraftClient client, int x, int y, int w, int h, int radius, int argb) {
		if (client == null || w <= 0 || h <= 0) return;
		if (((argb >> 24) & 0xFF) == 0) return;
		Plate plate = plate(client, w, h, radius, false, 1.75F);
		if (plate == null) return;
		drawPlate(client, plate, x, y, w, h, argb);
	}

	public static void outlineRound(MinecraftClient client, int x, int y, int w, int h, int radius, int argb) {
		if (client == null || w <= 0 || h <= 0) return;
		if (((argb >> 24) & 0xFF) == 0) return;
		int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));
		float strokeGui = 1.35F;
		if (r <= 0) {
			DrawableHelper.fill(x, y, x + w, y + 1, argb);
			DrawableHelper.fill(x, y + h - 1, x + w, y + h, argb);
			DrawableHelper.fill(x, y, x + 1, y + h, argb);
			DrawableHelper.fill(x + w - 1, y, x + w, y + h, argb);
			return;
		}
		Slice slice = slice(client, r, false, strokeGui);
		if (slice == null) return;
		GlStateManager.enableBlend();
		drawNine(client, slice, x, y, w, h, r, argb);
	}

	private static void ensureScale(MinecraftClient client) {
		int sf = Math.max(1, new Window(client).getScaleFactor());
		if (sf != bakedScale) {
			CACHE.clear();
			PLATES.clear();
			bakedScale = sf;
		}
	}

	private static Plate plate(MinecraftClient client, int w, int h, int radius, boolean filled, float strokePx) {
		ensureScale(client);
		int r = Math.max(0, Math.min(radius, Math.min(w / 2, h / 2)));
		int strokeKey = Math.round(strokePx * 100.0F);
		String key = bakedScale + "|" + w + "|" + h + "|" + r + "|" + filled + "|" + strokeKey;
		Plate cached = PLATES.get(key);
		if (cached != null) return cached;
		cached = bakePlate(client, w, h, r, filled, strokePx, "optic_ui_plate_" + PLATES.size());
		if (cached != null) PLATES.put(key, cached);
		return cached;
	}

	private static Plate bakePlate(MinecraftClient client, int w, int h, int radius, boolean filled, float strokePx, String name) {
		try {
			int sf = Math.max(1, bakedScale);
			int screenW = Math.max(1, w * sf);
			int screenH = Math.max(1, h * sf);
			int ss = 4;
			while ((screenW * ss > 2048 || screenH * ss > 2048) && ss > 1) ss /= 2;
			int imgW = screenW * ss;
			int imgH = screenH * ss;
			if (imgW <= 0 || imgH <= 0) return null;

			BufferedImage image = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

			// Keep the whole stroke, including its antialiased fringe, inside the texture.
			// A stroke that crosses the edge gets clipped into a stair-step.
			float stroke = Math.max(ss * 1.25F, strokePx * ss);
			float pad = filled ? 0.75F : stroke * 0.5F + 1.5F;
			float innerW = Math.max(1.0F, imgW - pad * 2.0F);
			float innerH = Math.max(1.0F, imgH - pad * 2.0F);
			float arc = Math.min(radius * sf * ss * 2.0F, Math.min(innerW, innerH));
			RoundRectangle2D.Float shape = new RoundRectangle2D.Float(pad, pad, innerW, innerH, arc, arc);
			g.setColor(Color.WHITE);
			if (filled) {
				g.fill(shape);
			} else {
				g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.draw(shape);
			}
			g.dispose();

			NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
			Identifier id = client.getTextureManager().registerDynamicTexture(name, texture);
			OpticSmooth.linearize(texture);
			return new Plate(id);
		} catch (Exception e) {
			System.err.println("[Optic] Plate bake failed: " + e.getMessage());
			return null;
		}
	}

	private static void drawPlate(MinecraftClient client, Plate plate, int x, int y, int w, int h, int argb) {
		float a = ((argb >> 24) & 0xFF) / 255.0F;
		float cr = ((argb >> 16) & 0xFF) / 255.0F;
		float cg = ((argb >> 8) & 0xFF) / 255.0F;
		float cb = (argb & 0xFF) / 255.0F;
		GlStateManager.enableBlend();
		OpticSmooth.bind(client, plate.id);
		GlStateManager.color(cr, cg, cb, a);
		DrawableHelper.drawTexture(x, y, 0.0F, 0.0F, w, h, (float) w, (float) h);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static Slice slice(MinecraftClient client, int radiusGui, boolean filled, float strokeGui) {
		ensureScale(client);
		String key = bakedScale + "|" + SUPER + "|" + radiusGui + "|" + filled + "|" + strokeGui;
		Slice cached = CACHE.get(key);
		if (cached != null) return cached;
		cached = bake(client, radiusGui, filled, strokeGui, "optic_ui_shape_" + CACHE.size());
		if (cached != null) CACHE.put(key, cached);
		return cached;
	}

	private static Slice bake(MinecraftClient client, int radiusGui, boolean filled, float strokeGui, String name) {
		try {
			int corner = Math.max(1, radiusGui);
			int texCorner = Math.max(corner * SUPER, 24);
			int pad = Math.max(4, Math.round(strokeGui * SUPER) + 4);
			int tex = texCorner * 2 + pad;
			BufferedImage image = new BufferedImage(tex, tex, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

			float inset = Math.max(1.0F, strokeGui * SUPER * 0.5F + 1.0F);
			float rw = tex - inset * 2.0F;
			float rh = tex - inset * 2.0F;
			float arc = texCorner * 2.0F;
			RoundRectangle2D.Float shape = new RoundRectangle2D.Float(inset, inset, rw, rh, arc, arc);
			if (filled) {
				g.setColor(Color.WHITE);
				g.fill(shape);
			} else {
				float stroke = Math.max(SUPER * strokeGui, 2.5F);
				g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.setColor(Color.WHITE);
				g.draw(shape);
			}
			g.dispose();

			NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
			Identifier id = client.getTextureManager().registerDynamicTexture(name, texture);
			OpticSmooth.linearize(texture);
			return new Slice(id, tex, texCorner, pad);
		} catch (Exception e) {
			System.err.println("[Optic] Shape bake failed: " + e.getMessage());
			return null;
		}
	}

	private static void drawNine(MinecraftClient client, Slice slice, int x, int y, int w, int h, int radius, int argb) {
		float a = ((argb >> 24) & 0xFF) / 255.0F;
		float cr = ((argb >> 16) & 0xFF) / 255.0F;
		float cg = ((argb >> 8) & 0xFF) / 255.0F;
		float cb = (argb & 0xFF) / 255.0F;

		int c = Math.max(1, radius);
		if (w < c * 2) c = Math.max(1, w / 2);
		if (h < c * 2) c = Math.max(1, h / 2);
		int midW = Math.max(0, w - c * 2);
		int midH = Math.max(0, h - c * 2);

		int edge = Math.max(1, slice.pad / 2);
		int u0 = edge;
		int u1 = edge + slice.texCorner;
		int u2 = slice.tex - edge - slice.texCorner;
		int u3 = slice.tex - edge;
		int v0 = u0;
		int v1 = u1;
		int v2 = u2;
		int v3 = u3;
		int midU = Math.max(1, u2 - u1);
		int midV = Math.max(1, v2 - v1);

		OpticSmooth.bind(client, slice.id);
		GlStateManager.color(cr, cg, cb, a);

		float inv = 1.0F / slice.tex;
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buf = tess.getBuffer();
		buf.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);

		quad(buf, x, y, c, c, u0, v0, slice.texCorner, slice.texCorner, inv);
		quad(buf, x + w - c, y, c, c, u2, v0, slice.texCorner, slice.texCorner, inv);
		quad(buf, x, y + h - c, c, c, u0, v2, slice.texCorner, slice.texCorner, inv);
		quad(buf, x + w - c, y + h - c, c, c, u2, v2, slice.texCorner, slice.texCorner, inv);

		if (midW > 0) {
			quad(buf, x + c, y, midW, c, u1, v0, midU, slice.texCorner, inv);
			quad(buf, x + c, y + h - c, midW, c, u1, v2, midU, slice.texCorner, inv);
		}
		if (midH > 0) {
			quad(buf, x, y + c, c, midH, u0, v1, slice.texCorner, midV, inv);
			quad(buf, x + w - c, y + c, c, midH, u2, v1, slice.texCorner, midV, inv);
		}
		if (midW > 0 && midH > 0) {
			quad(buf, x + c, y + c, midW, midH, u1, v1, midU, midV, inv);
		}

		tess.draw();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void quad(
		BufferBuilder buf,
		int x,
		int y,
		int w,
		int h,
		int u,
		int v,
		int uw,
		int vh,
		float inv
	) {
		float u0 = u * inv;
		float v0 = v * inv;
		float u1 = (u + uw) * inv;
		float v1 = (v + vh) * inv;
		buf.vertex(x, y + h, 0.0).texture(u0, v1).next();
		buf.vertex(x + w, y + h, 0.0).texture(u1, v1).next();
		buf.vertex(x + w, y, 0.0).texture(u1, v0).next();
		buf.vertex(x, y, 0.0).texture(u0, v0).next();
	}

	private static final class Plate {
		final Identifier id;

		Plate(Identifier id) {
			this.id = id;
		}
	}

	private static final class Slice {
		final Identifier id;
		final int tex;
		final int texCorner;
		final int pad;

		Slice(Identifier id, int tex, int texCorner, int pad) {
			this.id = id;
			this.tex = tex;
			this.texCorner = texCorner;
			this.pad = pad;
		}
	}
}
