package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.hud.OpticTitles;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class AnimationHudMixin {
	@ModifyVariable(method = "renderStatusBars", at = @At(value = "STORE"), ordinal = 0)
	private boolean optic$healthFlash(boolean flashing) {
		OpticConfig c = OpticClient.get().config();
		// 1.7 health animation = no white heart flash
		if (c.anim(c.animHealth)) return false;
		return flashing;
	}

	@Inject(
		method = "setTitles(Ljava/lang/String;Ljava/lang/String;III)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void optic$titlesToggle(String subtitle, String title, int fadeIn, int stay, int fadeOut, CallbackInfo ci) {
		OpticTitles.capture(subtitle, title, fadeIn, stay, fadeOut);
		OpticConfig c = OpticClient.get().config();
		// true is the 1.7 side of the switch: 1.8 titles did not exist, so hide them.
		// The titles module still wants the packet so it can draw its own overlay.
		if (c.anim(c.animTitles) && !c.titles) {
			ci.cancel();
		}
	}
}
