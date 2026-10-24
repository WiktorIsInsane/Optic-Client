package gg.optic.client.gui;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Window;
import org.lwjgl.opengl.GL11;

/** Downscale the title panorama and stretch it back so the menu background is soft. */
public final class OpticMenuBlur {
	private static Framebuffer mid;
	private static Framebuffer small;

	private OpticMenuBlur() {
	}

	public static void apply(MinecraftClient client) {
		if (client == null || !GLX.supportsFbo()) return;
		Framebuffer main = client.getFramebuffer();
		if (main == null) return;

		int pw = Math.max(1, client.width);
		int ph = Math.max(1, client.height);
		int mw = Math.max(32, pw / 7);
		int mh = Math.max(32, ph / 7);
		int sw = Math.max(20, pw / 18);
		int sh = Math.max(20, ph / 18);

		mid = prepare(mid, mw, mh);
		small = prepare(small, sw, sh);

		mid.bind(true);
		GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
		GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
		main.draw(mw, mh);

		small.bind(true);
		GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
		GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
		mid.draw(sw, sh);

		main.bind(true);
		small.draw(pw, ph);
		restoreGui(client);
	}

	private static Framebuffer prepare(Framebuffer buffer, int width, int height) {
		if (buffer == null) {
			buffer = new Framebuffer(width, height, false);
		} else if (buffer.textureWidth != width || buffer.textureHeight != height) {
			buffer.resize(width, height);
		}
		buffer.setTexFilter(9729);
		return buffer;
	}

	private static void restoreGui(MinecraftClient client) {
		Window window = new Window(client);
		client.getFramebuffer().bind(true);
		GlStateManager.viewport(0, 0, client.width, client.height);
		GlStateManager.matrixMode(5889);
		GlStateManager.loadIdentity();
		GlStateManager.ortho(0.0, window.getScaledWidth(), window.getScaledHeight(), 0.0, 1000.0, 3000.0);
		GlStateManager.matrixMode(5888);
		GlStateManager.loadIdentity();
		GlStateManager.translate(0.0F, 0.0F, -2000.0F);
		GlStateManager.enableTexture();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GlStateManager.enableAlphaTest();
		GlStateManager.disableDepthTest();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
