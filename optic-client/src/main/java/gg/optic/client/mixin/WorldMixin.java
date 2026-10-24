package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticEnvironment;
import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldMixin {
	@Shadow
	public Dimension dimension;

	@Inject(method = "getSkyAngle", at = @At("HEAD"), cancellable = true)
	private void optic$freezeTime(float tickDelta, CallbackInfoReturnable<Float> cir) {
		OpticConfig c = OpticClient.get().config();
		if (!OpticEnvironment.freezeTimeActive(c) || this.dimension == null) return;
		cir.setReturnValue(this.dimension.getSkyAngle(OpticEnvironment.frozenTimeOfDay(c), tickDelta));
	}

	@Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
	private void optic$rain(float offset, CallbackInfoReturnable<Float> cir) {
		float override = OpticEnvironment.rainOverride(OpticClient.get().config());
		if (override >= 0.0F) cir.setReturnValue(override);
	}

	@Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
	private void optic$thunder(float offset, CallbackInfoReturnable<Float> cir) {
		OpticConfig c = OpticClient.get().config();
		float thunder = OpticEnvironment.thunderOverride(c);
		if (thunder < 0.0F) return;
		float rain = OpticEnvironment.rainOverride(c);
		if (rain < 0.0F) rain = ((World) (Object) this).getRainGradient(offset);
		cir.setReturnValue(thunder * rain);
	}
}
