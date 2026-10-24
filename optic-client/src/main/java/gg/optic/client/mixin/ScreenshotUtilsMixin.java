package gg.optic.client.mixin;

import com.mojang.blaze3d.platform.GLX;
import gg.optic.client.OpticPresent;
import java.nio.IntBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenshotUtils.class)
public class ScreenshotUtilsMixin {
	@Redirect(
		method = "saveScreenshot(Ljava/io/File;Ljava/lang/String;IILnet/minecraft/client/gl/Framebuffer;)Lnet/minecraft/text/Text;",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GLX;supportsFbo()Z")
	)
	private static boolean optic$readWindow() {
		if (OpticPresent.direct(MinecraftClient.getInstance())) return false;
		return GLX.supportsFbo();
	}

	@Redirect(
		method = "saveScreenshot(Ljava/io/File;Ljava/lang/String;IILnet/minecraft/client/gl/Framebuffer;)Lnet/minecraft/text/Text;",
		at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glReadPixels(IIIIIILjava/nio/IntBuffer;)V", remap = false)
	)
	private static void optic$readFront(int x, int y, int width, int height, int format, int type, IntBuffer pixels) {
		boolean direct = OpticPresent.direct(MinecraftClient.getInstance());
		if (direct) GL11.glReadBuffer(GL11.GL_FRONT);
		GL11.glReadPixels(x, y, width, height, format, type, pixels);
		if (direct) GL11.glReadBuffer(GL11.GL_BACK);
	}
}
