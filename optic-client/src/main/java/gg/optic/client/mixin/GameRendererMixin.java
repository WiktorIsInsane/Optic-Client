package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticFreeLook;
import gg.optic.client.OpticZoom;
import gg.optic.client.render.OpticFrame;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "render(FJ)V", at = @At("HEAD"))
	private void optic$freeLookFrame(float tickDelta, long limitTime, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		OpticFreeLook.beforeRender(client);
		OpticFrame.look(client);
	}

	@Redirect(
		method = "transformCamera(F)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;yaw:F")
	)
	private float optic$freeLookYaw(Entity entity) {
		return OpticFreeLook.yaw(entity, entity.yaw);
	}

	@Redirect(
		method = "transformCamera(F)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevYaw:F")
	)
	private float optic$freeLookPrevYaw(Entity entity) {
		return OpticFreeLook.yaw(entity, entity.prevYaw);
	}

	@Redirect(
		method = "transformCamera(F)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;pitch:F")
	)
	private float optic$freeLookPitch(Entity entity) {
		return OpticFreeLook.pitch(entity, entity.pitch);
	}

	@Redirect(
		method = "transformCamera(F)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevPitch:F")
	)
	private float optic$freeLookPrevPitch(Entity entity) {
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

	/** Hurt Cam module off means the vanilla shake, as on Lunar. Only the module's toggle removes it. */
	@Inject(method = "bobViewWhenHurt", at = @At("HEAD"), cancellable = true)
	private void optic$disableHurtCam(float tickDelta, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (c.hurtCam && c.disableHurtCam) {
			ci.cancel();
		}
	}

	/**
	 * Minimal Bobbing: setupCamera and renderHand each check options.bobView before
	 * bobbing. Hiding it from setupCamera only keeps the held item bobbing while the
	 * world view stays level.
	 */
	@Redirect(
		method = "setupCamera(FI)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;bobView:Z")
	)
	private boolean optic$minimalBobbing(GameOptions options) {
		return options.bobView && !OpticClient.get().config().minimalBobbing;
	}

	/** Portal and nausea camera spin fights the crosshair. The overlay still draws. */
	@Redirect(
		method = "setupCamera(FI)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;timeInPortal:F")
	)
	private float optic$steadyPortal(ClientPlayerEntity player) {
		return 0.0F;
	}

	@Redirect(
		method = "setupCamera(FI)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;lastTimeInPortal:F")
	)
	private float optic$steadyPortalLast(ClientPlayerEntity player) {
		return 0.0F;
	}

	@ModifyConstant(method = "bobViewWhenHurt", constant = @Constant(floatValue = 14.0F))
	private float optic$hurtCamIntensity(float original) {
		OpticConfig c = OpticClient.get().config();
		if (!c.hurtCam) return original;
		return original * Math.max(0.0F, Math.min(2.0F, c.hurtCamIntensity));
	}

	/** Chunk rebuilds were allowed to eat a quarter of the frame. Cap that work at 1ms. */
	@ModifyVariable(method = "renderWorld(FJ)V", at = @At("HEAD"), argsOnly = true)
	private long optic$capChunkWork(long limitTime) {
		long cap = System.nanoTime() + 1_000_000L;
		return limitTime < cap ? limitTime : cap;
	}
}
