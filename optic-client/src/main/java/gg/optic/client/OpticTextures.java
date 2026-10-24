package gg.optic.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/** Loads Optic GUI textures from the mod jar (avoids 1.8.9 resource-pack lookup gaps). */
public final class OpticTextures {
	/** High-res bake; drawn smaller with GL_LINEAR for a smooth wheel. */
	public static final int COLOR_WHEEL_TEX = 256;
	/** On-screen draw size in the picker popup. */
	public static final int COLOR_WHEEL_DRAW = 128;

	private static final int LOGO_TEX = 512;

	private static Identifier logoId;
	private static int logoW = LOGO_TEX;
	private static int logoH = LOGO_TEX;
	private static Identifier colorWheelId;
	private static boolean attempted;
	private static boolean colorWheelAttempted;

	private OpticTextures() {
	}

	public static Identifier logo(MinecraftClient client) {
		if (logoId != null || attempted) return logoId;
		attempted = true;
		try (InputStream in = OpticTextures.class.getResourceAsStream("/assets/optic/textures/gui/logo.png")) {
			if (in == null) {
				System.err.println("[Optic] logo.png missing from classpath");
				return null;
			}
			BufferedImage image = ImageIO.read(in);
			if (image == null) {
				System.err.println("[Optic] logo.png could not be decoded");
				return null;
			}
			BufferedImage prepared = prepareLogo(image);
			logoW = prepared.getWidth();
			logoH = prepared.getHeight();
			NativeImageBackedTexture texture = new NativeImageBackedTexture(prepared);
			logoId = client.getTextureManager().registerDynamicTexture("optic_logo", texture);
			client.getTextureManager().bindTexture(logoId);
			try {
				int levels = 31 - Integer.numberOfLeadingZeros(Math.max(logoW, logoH));
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, Math.max(0, levels));
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
				GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
				texture.setFilter(true, true);
			} catch (Throwable ignored) {
				texture.setFilter(true, false);
			}
			System.out.println("[Optic] Logo texture ready: " + logoId + " " + logoW + "x" + logoH);
		} catch (Exception e) {
			System.err.println("[Optic] Failed to load logo: " + e.getMessage());
			e.printStackTrace();
		}
		return logoId;
	}

	public static int logoWidth() {
		return logoW;
	}

	public static int logoHeight() {
		return logoH;
	}

	/**
	 * Crop to the mark, then pad back to a square so the ring stays circular,
	 * and resample to a power-of-two so mipmaps stay sharp when drawn small.
	 */
	private static BufferedImage prepareLogo(BufferedImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		int minX = w;
		int minY = h;
		int maxX = 0;
		int maxY = 0;
		boolean any = false;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				if (((image.getRGB(x, y) >>> 24) & 0xFF) > 8) {
					if (x < minX) minX = x;
					if (y < minY) minY = y;
					if (x > maxX) maxX = x;
					if (y > maxY) maxY = y;
					any = true;
				}
			}
		}
		BufferedImage square = image;
		if (any && maxX > minX && maxY > minY) {
			long sumX = 0L;
			long sumY = 0L;
			long count = 0L;
			for (int y = minY; y <= maxY; y++) {
				for (int x = minX; x <= maxX; x++) {
					int argb = image.getRGB(x, y);
					int a = (argb >>> 24) & 0xFF;
					int r = (argb >> 16) & 0xFF;
					int g = (argb >> 8) & 0xFF;
					int b = argb & 0xFF;
					if (a > 40 && r > 180 && g > 180 && b > 180) {
						sumX += x;
						sumY += y;
						count++;
					}
				}
			}
			int cx = count > 0 ? (int) (sumX / count) : (minX + maxX) / 2;
			int cy = count > 0 ? (int) (sumY / count) : (minY + maxY) / 2;
			int reach = 0;
			for (int y = minY; y <= maxY; y++) {
				for (int x = minX; x <= maxX; x++) {
					if (((image.getRGB(x, y) >>> 24) & 0xFF) <= 8) continue;
					int cheb = Math.max(Math.abs(x - cx), Math.abs(y - cy));
					if (cheb > reach) reach = cheb;
				}
			}
			int side = (reach + 8) * 2 + 1;
			int half = side / 2;
			int x0 = cx - half;
			int y0 = cy - half;
			square = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
			Graphics2D crop = square.createGraphics();
			int sx = Math.max(0, x0);
			int sy = Math.max(0, y0);
			int dx = sx - x0;
			int dy = sy - y0;
			int sw = Math.min(side - dx, w - sx);
			int sh = Math.min(side - dy, h - sy);
			if (sw > 0 && sh > 0) {
				crop.drawImage(image, dx, dy, dx + sw, dy + sh, sx, sy, sx + sw, sy + sh, null);
			}
			crop.dispose();
		}
		if (square.getWidth() == LOGO_TEX && square.getHeight() == LOGO_TEX) return square;
		BufferedImage scaled = new BufferedImage(LOGO_TEX, LOGO_TEX, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = scaled.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.drawImage(square, 0, 0, LOGO_TEX, LOGO_TEX, null);
		g.dispose();
		return scaled;
	}

	/** HSV disk (hue=angle, sat=radius, value=1). Brightness is applied separately in the picker. */
	public static Identifier colorWheel(MinecraftClient client) {
		if (colorWheelId != null || colorWheelAttempted) return colorWheelId;
		colorWheelAttempted = true;
		try {
			int size = COLOR_WHEEL_TEX;
			BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			float center = (size - 1) * 0.5F;
			float maxR = center;
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					float dx = x - center;
					float dy = y - center;
					float dist = (float) Math.sqrt(dx * dx + dy * dy);
					if (dist > maxR) {
						image.setRGB(x, y, 0);
						continue;
					}
					// Soft anti-aliased edge
					float edge = Math.min(1.0F, Math.max(0.0F, (maxR - dist) * 1.5F));
					float sat = Math.min(1.0F, dist / maxR);
					float hue = (float) ((Math.atan2(dy, dx) + Math.PI) / (Math.PI * 2.0));
					int rgb = hsvToRgb(hue, sat, 1.0F);
					int alpha = Math.round(edge * 255.0F);
					image.setRGB(x, y, (alpha << 24) | rgb);
				}
			}
			NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
			colorWheelId = client.getTextureManager().registerDynamicTexture("optic_color_wheel", texture);
		} catch (Exception e) {
			System.err.println("[Optic] Failed to build color wheel: " + e.getMessage());
			e.printStackTrace();
		}
		return colorWheelId;
	}

	public static int hsvToRgb(float h, float s, float v) {
		h = h - (float) Math.floor(h);
		s = clamp01(s);
		v = clamp01(v);
		float c = v * s;
		float x = c * (1.0F - Math.abs((h * 6.0F) % 2.0F - 1.0F));
		float m = v - c;
		float r;
		float g;
		float b;
		int sector = (int) Math.floor(h * 6.0F) % 6;
		switch (sector) {
			case 0: r = c; g = x; b = 0; break;
			case 1: r = x; g = c; b = 0; break;
			case 2: r = 0; g = c; b = x; break;
			case 3: r = 0; g = x; b = c; break;
			case 4: r = x; g = 0; b = c; break;
			default: r = c; g = 0; b = x; break;
		}
		int ri = Math.round((r + m) * 255.0F);
		int gi = Math.round((g + m) * 255.0F);
		int bi = Math.round((b + m) * 255.0F);
		return (ri << 16) | (gi << 8) | bi;
	}

	public static float[] rgbToHsv(int rgb) {
		float r = ((rgb >> 16) & 0xFF) / 255.0F;
		float g = ((rgb >> 8) & 0xFF) / 255.0F;
		float b = (rgb & 0xFF) / 255.0F;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		float h = 0.0F;
		if (delta > 0.00001F) {
			if (max == r) {
				h = ((g - b) / delta) % 6.0F;
			} else if (max == g) {
				h = (b - r) / delta + 2.0F;
			} else {
				h = (r - g) / delta + 4.0F;
			}
			h /= 6.0F;
			if (h < 0.0F) h += 1.0F;
		}
		float s = max <= 0.00001F ? 0.0F : delta / max;
		return new float[] { h, s, max };
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}
}
