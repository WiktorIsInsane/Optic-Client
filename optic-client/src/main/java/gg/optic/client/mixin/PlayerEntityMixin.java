package gg.optic.client.mixin;

import gg.optic.client.render.OpticSneak;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
	@Inject(method = "tick()V", at = @At("TAIL"))
	private void optic$sneakPose(CallbackInfo ci) {
		OpticSneak.tick((PlayerEntity) (Object) this);
	}

	@Inject(method = "getEyeHeight()F", at = @At("RETURN"), cancellable = true)
	private void optic$smoothEye(CallbackInfoReturnable<Float> cir) {
		PlayerEntity player = (PlayerEntity) (Object) this;
		float amount = OpticSneak.visual(player);
		if (amount < 0.0F || player.isSleeping()) return;
		cir.setReturnValue(1.62F - 0.08F * amount);
	}
}
