package gg.optic.client.mixin;

import gg.optic.client.render.OpticSnap;
import net.minecraft.entity.player.OtherClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A hit keeps the position slide instead of collapsing it into one jump. */
@Mixin(OtherClientPlayerEntity.class)
public class OtherClientPlayerMixin {
	@Shadow
	private int field_1770;

	@Inject(method = "updateTrackedPositionAndAngles(DDDFFIZ)V", at = @At("TAIL"))
	private void optic$smoothKnockback(double x, double y, double z, float yaw, float pitch, int steps, boolean interpolate, CallbackInfo ci) {
		if (!OpticSnap.smooth((OtherClientPlayerEntity) (Object) this)) return;
		if (this.field_1770 < 3) this.field_1770 = 3;
	}
}
