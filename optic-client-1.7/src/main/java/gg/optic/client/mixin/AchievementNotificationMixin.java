package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import net.minecraft.advancement.Achievement;
import net.minecraft.client.gui.AchievementNotification;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Disable Achievements (General settings): the popup never shows. Achievements still unlock. */
@Mixin(AchievementNotification.class)
public abstract class AchievementNotificationMixin {
	@Inject(method = "display", at = @At("HEAD"), cancellable = true, require = 0)
	private void optic$hide(Achievement achievement, CallbackInfo ci) {
		if (OpticClient.get().config().disableAchievements) ci.cancel();
	}

	@Inject(method = "displayRaw", at = @At("HEAD"), cancellable = true, require = 0)
	private void optic$hideRaw(Achievement achievement, CallbackInfo ci) {
		if (OpticClient.get().config().disableAchievements) ci.cancel();
	}

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
	private void optic$hideTick(CallbackInfo ci) {
		if (OpticClient.get().config().disableAchievements) ci.cancel();
	}
}
