package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticFreeLook;
import gg.optic.client.OpticZoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.7 reads the camera rotation through LivingEntity (its camera entity type). 1.7.10 camera mods: FOV, Zoom, Hurt Cam, Free Look and Minimal Bobbing. Mouse timing stays vanilla. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "renderGui(F)V", at = @At("HEAD"))
	private void optic$freeLookFrame(float tickDelta, CallbackInfo ci) {
		OpticFreeLook.beforeRender(MinecraftClient.getInstance());
	}

	@Redirect(method = "transformCamera(F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;yaw:F"))
	private float optic$freeLookYaw(LivingEntity entity) {
		return OpticFreeLook.yaw(entity, entity.yaw);
	}

	@Redirect(method = "transformCamera(F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;prevYaw:F"))
	private float optic$freeLookPrevYaw(LivingEntity entity) {
		return OpticFreeLook.yaw(entity, entity.prevYaw);
	}

	@Redirect(method = "transformCamera(F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;pitch:F"))
	private float optic$freeLookPitch(LivingEntity entity) {
		return OpticFreeLook.pitch(entity, entity.pitch);
	}

	@Redirect(method = "transformCamera(F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;prevPitch:F"))
	private float optic$freeLookPrevPitch(LivingEntity entity) {
		return OpticFreeLook.pitch(entity, entity.prevPitch);
	}

	@Inject(method = "getFov(FZ)F", at = @At("RETURN"), cancellable = true)
	private void optic$zoomFov(float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		float value = cir.getReturnValueF();
		OpticConfig c = OpticClient.get().config();
		if (c.fovMod && changingFov) {
			MinecraftClient client = MinecraftClient.getInstance();
			float base = client != null && client.options != null ? client.options.fov : 0.0F;
			if (base > 20.0F) {
				float degrees = Math.max(30.0F, Math.min(150.0F, c.fov));
				value = value / base * degrees;
			}
		}
		float mult = OpticZoom.fovMultiplier();
		if (mult != 1.0F) value *= mult;
		cir.setReturnValue(value);
	}

	/** Hurt Cam module off means the vanilla shake. Only the module's toggle removes it. */
	@Inject(method = "bobViewWhenHurt(F)V", at = @At("HEAD"), cancellable = true)
	private void optic$disableHurtCam(float tickDelta, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (c.hurtCam && c.disableHurtCam) ci.cancel();
	}

	@ModifyConstant(method = "bobViewWhenHurt(F)V", constant = @Constant(floatValue = 14.0F))
	private float optic$hurtCamIntensity(float original) {
		OpticConfig c = OpticClient.get().config();
		if (!c.hurtCam) return original;
		return original * Math.max(0.0F, Math.min(2.0F, c.hurtCamIntensity));
	}

	/** Minimal Bobbing: hide bobView from the camera only, so the held item still bobs. */
	@Redirect(method = "setupCamera(FI)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;bobView:Z"))
	private boolean optic$minimalBobbing(GameOptions options) {
		return options.bobView && !OpticClient.get().config().minimalBobbing;
	}
}
