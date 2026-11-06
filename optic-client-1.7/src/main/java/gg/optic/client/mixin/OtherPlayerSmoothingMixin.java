package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import net.minecraft.entity.player.OtherClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Real-Time Opponents (visual only): 1.7 eases other players to each new server position over
 * 3 ticks (~150 ms), so you aim at where they were. With the setting on they reach it in one
 * tick. Nothing you send changes; their movement can look less smooth at high ping.
 */
@Mixin(OtherClientPlayerEntity.class)
public abstract class OtherPlayerSmoothingMixin {
	@ModifyVariable(method = "method_2488(DDDFFI)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int optic$realTimeOpponents(int steps) {
		OpticClient optic = OpticClient.get();
		if (optic != null && optic.config().realTimeOpponents && steps > 1) return 1;
		return steps;
	}
}
