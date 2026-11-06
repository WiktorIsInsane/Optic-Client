package gg.optic.client.mixin;

import gg.optic.client.render.OpticHitPredict;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hit Predict: when the server confirms a hurt we already showed, don't restart the flash. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHurtMixin {
	@Unique private int optic$hurtBefore;

	@Inject(method = "handleStatus(B)V", at = @At("HEAD"))
	private void optic$beforeStatus(byte status, CallbackInfo ci) {
		optic$hurtBefore = ((LivingEntity) (Object) this).hurtTime;
	}

	@Inject(method = "handleStatus(B)V", at = @At("TAIL"))
	private void optic$afterStatus(byte status, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (status == 2 && OpticHitPredict.keepPredicted(self, optic$hurtBefore)) self.hurtTime = optic$hurtBefore;
	}
}
