package gg.optic.client.mixin;

import gg.optic.client.render.OpticChatRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawableHelper.class)
public class DrawableHelperMixin {
	@Inject(method = "fillGradient(IIIIII)V", at = @At("HEAD"), cancellable = true)
	private void optic$skipTitleVignette(int x1, int y1, int x2, int y2, int color1, int color2, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && client.currentScreen instanceof TitleScreen) {
			ci.cancel();
		}
	}

	@Inject(method = "fill", at = @At("HEAD"), cancellable = true)
	private static void optic$chatBackgroundOpacity(int x1, int y1, int x2, int y2, int color, CallbackInfo ci) {
		if (OpticChatRender.reentry) return;
		float op = OpticChatRender.backgroundOpacity;
		if (op < 0.0F) return;
		// Chat message backgrounds are black (RGB 0); scrollbar fills have color.
		if ((color & 0x00FFFFFF) != 0) return;

		if (op <= 0.0F) {
			ci.cancel();
			return;
		}

		int a = (color >>> 24) & 0xFF;
		int na = Math.min(255, Math.round(a * (op / 100.0F)));
		if (na == a) return;

		ci.cancel();
		OpticChatRender.reentry = true;
		try {
			DrawableHelper.fill(x1, y1, x2, y2, na << 24);
		} finally {
			OpticChatRender.reentry = false;
		}
	}
}
