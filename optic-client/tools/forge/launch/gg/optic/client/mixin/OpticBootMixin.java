package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Starts Optic during Minecraft.initializeGame, whose notch name is ave.am. */
@Mixin(targets = "ave", remap = false)
public class OpticBootMixin {
	private static boolean optic$started;

	@Inject(method = "am()V", at = @At("HEAD"), remap = false)
	private void optic$boot(CallbackInfo ci) {
		if (optic$started) return;
		optic$started = true;
		OpticClient.get().onInitializeClient();
	}
}
