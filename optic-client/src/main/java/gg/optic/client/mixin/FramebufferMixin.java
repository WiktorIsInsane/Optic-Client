package gg.optic.client.mixin;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticPresent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Framebuffer.class)
public class FramebufferMixin {
	@Shadow public int viewportWidth;
	@Shadow public int viewportHeight;

	@Inject(method = "bind(Z)V", at = @At("HEAD"), cancellable = true)
	private void optic$directBind(boolean viewPort, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!OpticPresent.direct(client)) return;
		if ((Object) this != client.getFramebuffer()) return;
		GLX.advancedBindFramebuffer(GLX.framebuffer, 0);
		if (viewPort) {
			GlStateManager.viewport(0, 0, this.viewportWidth, this.viewportHeight);
		}
		ci.cancel();
	}

	@Inject(method = "draw(II)V", at = @At("HEAD"), cancellable = true)
	private void optic$skipBlit(int width, int height, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!OpticPresent.direct(client)) return;
		if ((Object) this != client.getFramebuffer()) return;
		ci.cancel();
	}
}
