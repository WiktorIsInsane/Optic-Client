package gg.optic.client;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;

/**
 * Keeps Optic screens on Minecraft's normal GUI size.
 * Only turns off the alpha test that was chopping smooth edges into pixels.
 */
public final class OpticFrame {
	private OpticFrame() {
	}

	public static void begin(MinecraftClient client) {
		GlStateManager.disableAlphaTest();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	public static void end() {
		GlStateManager.enableAlphaTest();
	}
}
