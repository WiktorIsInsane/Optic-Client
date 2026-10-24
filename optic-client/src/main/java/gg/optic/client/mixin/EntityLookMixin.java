package gg.optic.client.mixin;

import gg.optic.client.OpticFreeLook;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mouse look during free look stays on the camera. The player yaw the server hears is left alone. */
@Mixin(Entity.class)
public class EntityLookMixin {
	@Inject(method = "increaseTransforms(FF)V", at = @At("HEAD"), cancellable = true)
	private void optic$freeLook(float yawDelta, float pitchDelta, CallbackInfo ci) {
		if (!((Object) this instanceof ClientPlayerEntity)) return;
		if (OpticFreeLook.applyLook(yawDelta, pitchDelta)) ci.cancel();
	}
}
