package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.render.OpticHitColor;
import gg.optic.client.render.OpticNametags;
import gg.optic.client.render.OpticSkins3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BiPedModel;
import net.minecraft.client.render.entity.model.EntityModel;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hit Color on 1.7.10. 1.7 draws the hurt overlay as a second model pass with
 * glColor4f(brightness, 0, 0, 0.4); swap that colour for the custom one.
 * Matched by value so it still finds the calls if OptiFine reorders the method.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Redirect(
		method = "render(Lnet/minecraft/entity/LivingEntity;DDDFF)V",
		at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V", remap = false)
	)
	private void optic$hitColor(float red, float green, float blue, float alpha) {
		boolean hurtTint = green == 0.0F && blue == 0.0F && Math.abs(alpha - 0.4F) < 0.001F;
		if (hurtTint && OpticHitColor.enabled()) {
			// red carries the lighting brightness for this pass; keep it on the custom colour.
			GL11.glColor4f(OpticHitColor.red() * red, OpticHitColor.green() * red, OpticHitColor.blue() * red, OpticHitColor.alpha());
			return;
		}
		GL11.glColor4f(red, green, blue, alpha);
	}

	@Shadow protected EntityModel model;

	/** 3D Skins: hide the flat hat before the player model draws. */
	@Inject(method = "renderModel", at = @At("HEAD"), require = 0)
	private void optic$skins3dBefore(LivingEntity entity, float a, float b, float c, float d, float e, float scale, CallbackInfo ci) {
		if (!((Object) this instanceof PlayerEntityRenderer) || !(this.model instanceof BiPedModel)) return;
		OpticSkins3D.before(entity, (BiPedModel) this.model);
	}

	/** 3D Skins: voxel outer layer with the part angles the model just used. */
	@Inject(method = "renderModel", at = @At("TAIL"), require = 0)
	private void optic$skins3dAfter(LivingEntity entity, float a, float b, float c, float d, float e, float scale, CallbackInfo ci) {
		if (!((Object) this instanceof PlayerEntityRenderer) || !(this.model instanceof BiPedModel)) return;
		OpticSkins3D.after(entity, (BiPedModel) this.model, scale);
	}

	/**
	 * 1.7 draws sneaking players' tags inline at 1/8 opacity (practically invisible). With the
	 * Nametags module on, send them through renderLabelIfPresent instead so OpticNametags
	 * draws them (readable, but hidden behind walls). ordinal 1 is the branch check; ordinal 0
	 * is the 32-block range for sneaking players, which stays.
	 */
	@Redirect(
		method = "method_5782(Lnet/minecraft/entity/LivingEntity;DDD)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isSneaking()Z", ordinal = 1),
		require = 0
	)
	private boolean optic$sneakLabel(LivingEntity entity) {
		if (OpticClient.get().config().nametags) return false;
		return entity.isSneaking();
	}

	/** Nametags module visibility. method_5781 is 1.7's hasLabel; 1.7 has no team nametag rules. */
	@Inject(method = "method_5781", at = @At("HEAD"), cancellable = true)
	private void optic$nametagVisibility(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		OpticConfig c = OpticClient.get().config();
		if (!c.nametags) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.field_3805 == null) return;
		if (OpticNametags.allHidden() || (client.options.hudHidden && c.nametagsHideInF1)) {
			cir.setReturnValue(false);
			return;
		}
		if (entity == client.field_3805) {
			cir.setReturnValue(c.nametagsShowInF5 && client.options.perspective != 0 && entity.rider == null);
			return;
		}
		if (entity instanceof PlayerEntity && OpticNametags.playersHidden()) cir.setReturnValue(false);
	}
}
