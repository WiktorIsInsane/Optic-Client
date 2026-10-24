package gg.optic.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.mixin.OpticPanoramaAccess;
import gg.optic.client.mixin.OpticScreenAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.Window;

/**
 * The title-screen world, darkened so the menu reads on top of it.
 */
public final class OpticWorldBackground {
	private static final int DARK = 0xE610100C;
	private static TitleScreen panorama;
	private static long lastDrawNs;
	private static long lastTurnNs;
	private static boolean failed;

	private OpticWorldBackground() {
	}

	public static void draw(MinecraftClient client, int width, int height) {
		long now = System.nanoTime();
		if (now - lastDrawNs < 12_000_000L) return;
		lastDrawNs = now;
		if (client == null || width < 1 || height < 1) return;
		if (!failed) {
			try {
				drawWorld(client, width, height, now);
			} catch (Throwable error) {
				failed = true;
				System.out.println("[Optic] World background unavailable: " + error);
			}
		}
		DrawableHelper.fill(0, 0, width, height, DARK);
	}

	private static void drawWorld(MinecraftClient client, int width, int height, long now) {
		if (panorama == null) {
			panorama = new TitleScreen();
		}
		OpticPanoramaAccess access = (OpticPanoramaAccess) panorama;
		((OpticScreenAccess) panorama).optic$setClient(client);
		panorama.width = width;
		panorama.height = height;
		if (access.optic$getBackgroundTexture() == null) {
			NativeImageBackedTexture texture = new NativeImageBackedTexture(256, 256);
			access.optic$setBackgroundTexture(texture);
			access.optic$setBackgroundTextureId(client.getTextureManager().registerDynamicTexture("optic_world", texture));
		}
		if (now - lastTurnNs > 50_000_000L) {
			access.optic$setTicks(access.optic$getTicks() + 1);
			lastTurnNs = now;
		}
		access.optic$renderPanorama(0, 0, 0.0F);
		restoreGui(client);
	}

	private static void restoreGui(MinecraftClient client) {
		Window window = new Window(client);
		if (client.getFramebuffer() != null) {
			client.getFramebuffer().bind(true);
		}
		GlStateManager.viewport(0, 0, client.width, client.height);
		GlStateManager.matrixMode(5889);
		GlStateManager.loadIdentity();
		GlStateManager.ortho(0.0, window.getScaledWidth(), window.getScaledHeight(), 0.0, 1000.0, 3000.0);
		GlStateManager.matrixMode(5888);
		GlStateManager.loadIdentity();
		GlStateManager.translate(0.0F, 0.0F, -2000.0F);
		GlStateManager.colorMask(true, true, true, true);
		GlStateManager.depthMask(true);
		GlStateManager.enableTexture();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GlStateManager.enableAlphaTest();
		GlStateManager.disableDepthTest();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
