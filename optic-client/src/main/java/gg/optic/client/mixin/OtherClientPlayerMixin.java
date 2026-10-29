package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.render.OpticSnap;
import net.minecraft.entity.player.OtherClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OtherClientPlayerEntity.class)
public class OtherClientPlayerMixin {
	@Shadow
	private int field_1770;

	/**
	 * Vanilla eases every position update for other players over 3 ticks, so a hit's
	 * knockback shows up to 150 ms late. The server only sends player positions every
	 * 2 ticks, so easing over 1 tick made fast movers jump and pause. Real-Time Players
	 * eases over 2 ticks: one update ends as the next arrives, 50 ms sooner than vanilla
	 * with no gaps. Drawing only; nothing sent changes.
	 */
	@Inject(method = "updateTrackedPositionAndAngles(DDDFFIZ)V", at = @At("TAIL"))
	private void optic$smoothKnockback(double x, double y, double z, float yaw, float pitch, int steps, boolean interpolate, CallbackInfo ci) {
		if (OpticClient.get().config().realTimePlayers) {
			if (this.field_1770 > 2) this.field_1770 = 2;
			return;
		}
		if (!OpticSnap.smooth((OtherClientPlayerEntity) (Object) this)) return;
		if (this.field_1770 < 3) this.field_1770 = 3;
	}
}
