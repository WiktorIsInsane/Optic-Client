package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.render.OpticHitColor;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorFeatureRenderer.class)
public class ArmorFeatureMixin {
	@Inject(
		method = "renderFeature(Lnet/minecraft/entity/LivingEntity;FFFFFFFI)V",
		at = @At("HEAD")
	)
	private void optic$armorEntity(
		LivingEntity entity,
		float handSwing,
		float handSwingAmount,
		float tickDelta,
		float age,
		float headYaw,
		float headPitch,
		float scale,
		int slot,
		CallbackInfo ci
	) {
		OpticHitColor.armorEntity = entity;
	}

	@Inject(
		method = "renderFeature(Lnet/minecraft/entity/LivingEntity;FFFFFFFI)V",
		at = @At("RETURN")
	)
	private void optic$clearArmorEntity(
		LivingEntity entity,
		float handSwing,
		float handSwingAmount,
		float tickDelta,
		float age,
		float headYaw,
		float headPitch,
		float scale,
		int slot,
		CallbackInfo ci
	) {
		OpticHitColor.armorEntity = null;
	}

	@ModifyArg(
		method = "renderFeature(Lnet/minecraft/entity/LivingEntity;FFFFFFFI)V",
		require = 0,
		at = @At(
			value = "INVOKE",
			target = "Lbfl;c(FFFF)V",
			ordinal = 0,
			remap = false
		),
		index = 0
	)
	private float optic$armorRed(float original) {
		return OpticHitColor.tintArmor() ? OpticHitColor.red() : original;
	}

	@ModifyArg(
		method = "renderFeature(Lnet/minecraft/entity/LivingEntity;FFFFFFFI)V",
		require = 0,
		at = @At(
			value = "INVOKE",
			target = "Lbfl;c(FFFF)V",
			ordinal = 0,
			remap = false
		),
		index = 1
	)
	private float optic$armorGreen(float original) {
		return OpticHitColor.tintArmor() ? OpticHitColor.green() : original;
	}

	@ModifyArg(
		method = "renderFeature(Lnet/minecraft/entity/LivingEntity;FFFFFFFI)V",
		require = 0,
		at = @At(
			value = "INVOKE",
			target = "Lbfl;c(FFFF)V",
			ordinal = 0,
			remap = false
		),
		index = 2
	)
	private float optic$armorBlue(float original) {
		return OpticHitColor.tintArmor() ? OpticHitColor.blue() : original;
	}

	@Inject(method = "combineTextures()Z", at = @At("HEAD"), cancellable = true)
	private void optic$damageRedArmor(CallbackInfoReturnable<Boolean> cir) {
		OpticConfig c = OpticClient.get().config();
		if (c.anim(c.animDamage)) {
			cir.setReturnValue(true);
		}
	}
}
