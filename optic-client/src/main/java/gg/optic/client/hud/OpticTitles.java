package gg.optic.client.hud;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.Display;

/**
 * Server title / subtitle overlay, plus the operating-system window title bar.
 */
public final class OpticTitles {
	private static String title = "";
	private static String subtitle = "";
	private static int fadeIn = 10;
	private static int stay = 70;
	private static int fadeOut = 20;
	private static int ticks;
	private static long lastWindowTitle;

	private OpticTitles() {
	}

	public static void capture(String nextSubtitle, String nextTitle, int in, int hold, int out) {
		title = nextTitle == null ? "" : nextTitle;
		subtitle = nextSubtitle == null ? "" : nextSubtitle;
		fadeIn = Math.max(0, in);
		stay = Math.max(0, hold);
		fadeOut = Math.max(0, out);
		ticks = fadeIn + stay + fadeOut;
	}

	public static void tick(MinecraftClient client) {
		if (ticks > 0) ticks--;
		OpticConfig c = OpticClient.get().config();
		if (!c.titles || !c.windowTitle || client == null) return;
		long now = System.currentTimeMillis();
		if (now - lastWindowTitle < 500L) return;
		lastWindowTitle = now;
		String text = "Optic";
		if (c.windowTitleMode >= 1) text = text + " | " + MinecraftClient.getCurrentFps() + " FPS";
		if (c.windowTitleMode >= 2) {
			int ping = OpticPing.read(client);
			text = text + " | " + (ping < 0 ? "..." : ping + " ms");
		}
		Display.setTitle(text);
	}

	public static boolean drawing() {
		OpticConfig c = OpticClient.get().config();
		return c.titles && ticks > 0 && (title.length() > 0 || subtitle.length() > 0);
	}

	public static void render(MinecraftClient client, int screenW, int screenH, boolean editor) {
		OpticConfig c = OpticClient.get().config();
		if (!c.titles) return;
		String main = editor ? "Title" : title;
		String sub = editor ? "subtitle" : subtitle;
		int life = editor ? stay + fadeIn : ticks;
		if (!editor && life <= 0) return;
		if ((main == null || main.length() == 0) && (sub == null || sub.length() == 0) && !editor) return;

		int alpha = 255;
		if (!editor) {
			int ageLeft = life;
			if (ageLeft <= fadeOut && fadeOut > 0) alpha = ageLeft * 255 / fadeOut;
			else if (ageLeft > stay + fadeOut && fadeIn > 0) {
				int into = fadeIn + stay + fadeOut - ageLeft;
				alpha = into * 255 / fadeIn;
			}
			if (alpha < 8) return;
			if (alpha > 255) alpha = 255;
		}

		float scale = Math.max(0.5F, c.titlesScale);
		int titleW = main == null ? 0 : Math.round(client.textRenderer.getStringWidth(main) * scale * 2.0F);
		int subW = sub == null ? 0 : Math.round(client.textRenderer.getStringWidth(sub) * scale);
		int blockW = Math.max(titleW, subW);
		int blockH = Math.round((c.titlesShowTitle ? 18.0F : 0.0F) * scale + (c.titlesShowSubtitle ? 12.0F : 0.0F) * scale);
		int x;
		int y;
		if (c.titlesCentered || c.titlesX < 0) {
			x = screenW / 2 - blockW / 2;
			y = screenH / 2 - 30 + c.titlesY;
		} else {
			x = c.titlesX;
			y = c.titlesY;
		}

		if (c.titlesShowTitle && main != null && main.length() > 0) {
			drawScaled(client, main, x + (blockW - titleW) / 2, y, c.titlesTitleColor, alpha, scale * 2.0F);
		}
		if (c.titlesShowSubtitle && sub != null && sub.length() > 0) {
			int subY = y + Math.round(18.0F * scale);
			drawScaled(client, sub, x + (blockW - subW) / 2, subY, c.titlesSubtitleColor, alpha, scale);
		}
	}

	public static int[] editorSize(MinecraftClient client, OpticConfig c) {
		float scale = Math.max(0.5F, c.titlesScale);
		int titleW = Math.round(client.textRenderer.getStringWidth("Title") * scale * 2.0F);
		int subW = Math.round(client.textRenderer.getStringWidth("subtitle") * scale);
		int h = Math.round((c.titlesShowTitle ? 18.0F : 0.0F) * scale + (c.titlesShowSubtitle ? 12.0F : 0.0F) * scale);
		return new int[] { Math.max(titleW, subW), Math.max(10, h) };
	}

	public static int editorX(MinecraftClient client, OpticConfig c, int screenW) {
		if (c.titlesCentered || c.titlesX < 0) return screenW / 2 - editorSize(client, c)[0] / 2;
		return c.titlesX;
	}

	public static int editorY(OpticConfig c, int screenH) {
		if (c.titlesCentered || c.titlesX < 0) return screenH / 2 - 30 + c.titlesY;
		return c.titlesY;
	}

	private static void drawScaled(MinecraftClient client, String text, int x, int y, int rgb, int alpha, float scale) {
		int color = (alpha << 24) | (rgb & 0xFFFFFF);
		com.mojang.blaze3d.platform.GlStateManager.pushMatrix();
		com.mojang.blaze3d.platform.GlStateManager.translate(x, y, 0.0F);
		com.mojang.blaze3d.platform.GlStateManager.scale(scale, scale, 1.0F);
		com.mojang.blaze3d.platform.GlStateManager.enableBlend();
		client.textRenderer.draw(text, 0, 0, color, true);
		com.mojang.blaze3d.platform.GlStateManager.popMatrix();
	}

	public static void restoreWindowTitle() {
		Display.setTitle("Optic 1.8.9");
		lastWindowTitle = 0L;
	}
}
