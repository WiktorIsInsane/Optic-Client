package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** PvP Tweaks module: first-person overlays (low fire, clear water). Visual only. */
@Mixin(HeldItemRenderer.class)
public abstract class PvpTweaksMixin {
	private static OpticConfig optic$config() {
		OpticConfig c = OpticClient.get().config();
		return c.pvpTweaks ? c : null;
	}

	/** Low Fire: the burning overlay sits lower so it doesn't cover the middle of the screen. */
	@Inject(method = "renderFireOverlay(F)V", at = @At("HEAD"), require = 0)
	private void optic$lowFireBegin(float tickDelta, CallbackInfo ci) {
		OpticConfig c = optic$config();
		GL11.glPushMatrix();
		if (c != null && c.pvpLowFire) GL11.glTranslatef(0.0F, -0.3F, 0.0F);
	}

	@Inject(method = "renderFireOverlay(F)V", at = @At("RETURN"), require = 0)
	private void optic$lowFireEnd(float tickDelta, CallbackInfo ci) {
		GL11.glPopMatrix();
	}

	/** Clear Water: no dark underwater texture over the view. */
	@Inject(method = "renderUnderwaterOverlay(F)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void optic$clearWater(float tickDelta, CallbackInfo ci) {
		OpticConfig c = optic$config();
		if (c != null && c.pvpClearWater) ci.cancel();
	}

	/** HUD overlays: pumpkin, vignette and the nether portal swirl. */
	@Mixin(InGameHud.class)
	public abstract static class HudOverlays {
		@Inject(method = "renderPumpkinBlur(II)V", at = @At("HEAD"), cancellable = true, require = 0)
		private void optic$noPumpkin(int width, int height, CallbackInfo ci) {
			OpticConfig c = OpticClient.get().config();
			if (c.pvpTweaks && c.pvpNoPumpkin) ci.cancel();
		}

		@Inject(method = "renderVignette(FII)V", at = @At("HEAD"), cancellable = true, require = 0)
		private void optic$noVignette(float brightness, int width, int height, CallbackInfo ci) {
			OpticConfig c = OpticClient.get().config();
			if (c.pvpTweaks && c.pvpNoVignette) ci.cancel();
		}

		@Inject(method = "method_5589(FII)V", at = @At("HEAD"), cancellable = true, require = 0)
		private void optic$noPortal(float strength, int width, int height, CallbackInfo ci) {
			OpticConfig c = OpticClient.get().config();
			if (c.pvpTweaks && c.pvpNoPortal) ci.cancel();
		}
	}
}
