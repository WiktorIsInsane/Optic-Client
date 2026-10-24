package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.8 {@code LivingEntity.getRotationVector} aims with {@code headYaw}, which the
 * local player only copies from {@code yaw} once per tick. The crosshair ray then
 * trails the camera by up to 50 ms. For the local player, aim with the camera
 * yaw/pitch like {@code Entity} does. Reach and targeting are unchanged.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityLookMixin {
	@Inject(method = "getRotationVector(F)Lnet/minecraft/util/math/Vec3d;", at = @At("HEAD"), cancellable = true)
	private void optic$cameraLook(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
		if (!((Object) this instanceof ClientPlayerEntity)) return;
		if (!OpticClient.get().config().mouseDelayFix) return;
		Entity self = (Entity) (Object) this;
		float pitch = self.pitch;
		float yaw = self.yaw;
		if (tickDelta != 1.0F) {
			pitch = self.prevPitch + (self.pitch - self.prevPitch) * tickDelta;
			yaw = self.prevYaw + (self.yaw - self.prevYaw) * tickDelta;
		}
		float yawCos = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
		float yawSin = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
		float pitchCos = -MathHelper.cos(-pitch * 0.017453292F);
		float pitchSin = MathHelper.sin(-pitch * 0.017453292F);
		cir.setReturnValue(new Vec3d(yawSin * pitchCos, pitchSin, yawCos * pitchCos));
	}
}
