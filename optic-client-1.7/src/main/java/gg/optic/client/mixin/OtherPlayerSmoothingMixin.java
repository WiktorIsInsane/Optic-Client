package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import net.minecraft.entity.player.OtherClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Real-Time Opponents (visual only): 1.7 eases other players to each new server position over
 * 3 ticks (~150 ms), so you aim at where they were. With the setting on they reach it in one
 * tick. Nothing you send changes; their movement can look less smooth at high ping.
 *
 * Sets the step count after the method stored it (changing the argument itself broke on this
 * method's mix of double and int parameters).
 */
@Mixin(OtherClientPlayerEntity.class)
public abstract class OtherPlayerSmoothingMixin {
	/** Ticks left to reach the latest server position. */
	@Shadow private int field_1770;

	@Inject(method = "method_2488(DDDFFI)V", at = @At("TAIL"))
	private void optic$realTimeOpponents(double x, double y, double z, float yaw, float pitch, int steps, CallbackInfo ci) {
		OpticClient optic = OpticClient.get();
		if (optic != null && optic.config().realTimeOpponents && this.field_1770 > 1) this.field_1770 = 1;
	}
}
