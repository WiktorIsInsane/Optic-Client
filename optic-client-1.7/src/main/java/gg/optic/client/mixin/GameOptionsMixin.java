package gg.optic.client.mixin;

import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The game always launches windowed: a saved "fullscreen:true" is dropped when options load. */
@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
	@Shadow public boolean fullscreen;

	@Inject(method = "load()V", at = @At("TAIL"))
	private void optic$windowedOnLoad(CallbackInfo ci) {
		fullscreen = false;
	}
}