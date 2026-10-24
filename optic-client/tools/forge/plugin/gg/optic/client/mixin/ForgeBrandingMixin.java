package gg.optic.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the Forge / MCP / OptiFine / mod-count lines on the main menu. */
@Mixin(targets = "net.minecraftforge.client.ForgeHooksClient", remap = false)
public class ForgeBrandingMixin {
	@Inject(method = "renderMainMenu", at = @At("HEAD"), cancellable = true, remap = false)
	private static void optic$hideBranding(CallbackInfo ci) {
		ci.cancel();
	}
}
