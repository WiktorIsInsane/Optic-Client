package gg.optic.client.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.render.OpticSneak;
import net.minecraft.client.render.entity.model.BiPedModel;
import net.minecraft.client.render.model.ModelPart;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BiPedModel.class)
public class BiPedModelMixin {
	@Shadow public ModelPart body;
	@Shadow public ModelPart head;
	@Shadow public ModelPart rightArm;
	@Shadow public ModelPart leftArm;
	@Shadow public ModelPart rightLeg;
	@Shadow public ModelPart leftLeg;
	@Shadow public boolean sneaking;

	@ModifyConstant(method = "setAngles", constant = @Constant(floatValue = -0.5235988F))
	private float optic$oldBlockArm(float value) {
		OpticConfig c = OpticClient.get().config();
		return c.anim(c.animBlockHit) ? 0.0F : value;
	}

	/**
	 * Vanilla writes the full crouch or the full stand. Slide between those
	 * poses so 1.7 sneaking eases instead of popping.
	 */
	@Inject(method = "setAngles", at = @At("RETURN"))
	private void optic$easeSneak(
		float limbAngle,
		float limbDistance,
		float animationProgress,
		float headYaw,
		float headPitch,
		float scale,
		Entity entity,
		CallbackInfo ci
	) {
		float amount = OpticSneak.visual(entity);
		if (amount < 0.0F) return;
		float delta = amount - (this.sneaking ? 1.0F : 0.0F);
		if (delta == 0.0F) return;
		this.body.posX += 0.5F * delta;
		this.rightArm.posX += 0.4F * delta;
		this.leftArm.posX += 0.4F * delta;
		this.rightLeg.pivotZ += 3.9F * delta;
		this.leftLeg.pivotZ += 3.9F * delta;
		this.rightLeg.pivotY += -3.0F * delta;
		this.leftLeg.pivotY += -3.0F * delta;
		this.head.pivotY += delta;
	}

	@Redirect(
		method = "render(Lnet/minecraft/entity/Entity;FFFFFF)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z")
	)
	private boolean optic$sneakDrop(Entity entity) {
		float amount = OpticSneak.visual(entity);
		if (amount < 0.0F) return entity.isSneaking();
		return amount > 0.001F;
	}

	@Redirect(
		method = "render(Lnet/minecraft/entity/Entity;FFFFFF)V",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/platform/GlStateManager;translate(FFF)V"
		)
	)
	private void optic$sneakDropAmount(float x, float y, float z) {
		if (x == 0.0F && z == 0.0F && Math.abs(y - 0.2F) < 0.001F && this.optic$lastSneak >= 0.0F) {
			y = 0.2F * this.optic$lastSneak;
		}
		GlStateManager.translate(x, y, z);
	}

	@org.spongepowered.asm.mixin.Unique
	private float optic$lastSneak = -1.0F;

	@Inject(method = "render(Lnet/minecraft/entity/Entity;FFFFFF)V", at = @At("HEAD"))
	private void optic$rememberSneak(Entity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, float scale, CallbackInfo ci) {
		this.optic$lastSneak = OpticSneak.visual(entity);
	}
}
