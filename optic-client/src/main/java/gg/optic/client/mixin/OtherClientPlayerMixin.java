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
	 * knockback shows up to 150 ms late. Real-Time Players eases over 1 tick: the
	 * player still moves smoothly between frames but reaches each server position on
	 * the next tick. Drawing only; nothing sent changes.
	 */
	@Inject(method = "updateTrackedPositionAndAngles(DDDFFIZ)V", at = @At("TAIL"))
	private void optic$smoothKnockback(double x, double y, double z, float yaw, float pitch, int steps, boolean interpolate, CallbackInfo ci) {
		if (OpticClient.get().config().realTimePlayers) {
			this.field_1770 = 1;
			return;
		}
		if (!OpticSnap.smooth((OtherClientPlayerEntity) (Object) this)) return;
		if (this.field_1770 < 3) this.field_1770 = 3;
	}
}
