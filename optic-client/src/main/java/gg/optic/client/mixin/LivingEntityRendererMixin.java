package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.render.OpticNametags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {
	@Shadow
	protected abstract boolean hasLabel(T livingEntity);

	/** Skip the fall-over pose on hits and on a real death. */
	@Redirect(
		method = "method_5777",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/entity/LivingEntity;deathTime:I",
			opcode = Opcodes.GETFIELD
		)
	)
	private int optic$noDeathAnimation(LivingEntity entity) {
		return 0;
	}

	@Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
	private void optic$nametagVisibility(T livingEntity, CallbackInfoReturnable<Boolean> cir) {
		OpticConfig c = OpticClient.get().config();
		if (!c.nametags) return;

		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity self = client.player;
		if (self == null) return;

		boolean hudHidden = client.options.hudHidden;
		if (hudHidden && !c.nametagsShowInF1) {
			cir.setReturnValue(false);
			return;
		}

		boolean isSelf = livingEntity == self;
		boolean thirdPerson = client.options.perspective != 0;
		if (isSelf) {
			cir.setReturnValue(c.nametagsShowInF5 && thirdPerson && livingEntity.rider == null);
			return;
		}

		if (livingEntity instanceof PlayerEntity) {
			AbstractTeam team = livingEntity.getScoreboardTeam();
			AbstractTeam selfTeam = self.getScoreboardTeam();
			if (team != null) {
				AbstractTeam.VisibilityRule rule = team.getNameTagVisibilityRule();
				switch (rule) {
					case NEVER:
						cir.setReturnValue(false);
						return;
					case HIDE_FOR_OTHER_TEAMS:
						if (selfTeam != null && !team.isEqual(selfTeam)) {
							cir.setReturnValue(false);
							return;
						}
						break;
					case HIDE_FOR_OWN_TEAM:
						if (selfTeam != null && team.isEqual(selfTeam)) {
							cir.setReturnValue(false);
							return;
						}
						break;
					default:
						break;
				}
			}
		}

		Entity camera = client.getCameraEntity();
		if (camera == null) camera = self;
		cir.setReturnValue(
			livingEntity != camera
				&& !livingEntity.isInvisibleTo(self)
				&& livingEntity.rider == null
		);
	}

	@Inject(method = "method_10256", at = @At("HEAD"), cancellable = true)
	private void optic$sneakNametag(T livingEntity, double x, double y, double z, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.nametags || !livingEntity.isSneaking()) return;
		if (!this.hasLabel(livingEntity)) return;

		MinecraftClient client = MinecraftClient.getInstance();
		EntityRenderDispatcher dispatcher = client.getEntityRenderManager();
		Entity camera = client.getCameraEntity();
		if (camera == null) camera = client.player;
		if (camera == null) return;

		double dist = livingEntity.squaredDistanceTo(camera);
		float range = 32.0F;
		if (dist >= range * range) return;

		ci.cancel();
		String text = livingEntity.getName().asFormattedString();
		double yy = y - (livingEntity.isBaby() ? livingEntity.height / 2.0 : 0.0);
		OpticNametags.render(dispatcher, livingEntity, text, x, yy, z, 64);
	}
}
