package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.hud.OpticPing;
import gg.optic.client.render.OpticHitColor;
import gg.optic.client.render.OpticNametags;
import gg.optic.client.render.OpticSneak;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;

import java.nio.FloatBuffer;
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

	@Shadow
	protected FloatBuffer buffer;

	@Inject(method = "render(Lnet/minecraft/entity/LivingEntity;DDDFF)V", at = @At("HEAD"))
	private void optic$sneakFrame(T livingEntity, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
		OpticSneak.frame(tickDelta);
	}

	/**
	 * Yarn builds upload the hurt tint from renderModel. Official 1.8.9 uploads it
	 * from setBrightness ({@code a(LivingEntity, float, boolean)}). Either inject
	 * is allowed to miss so a missing call site cannot fail class init.
	 */
	@Inject(
		method = "renderModel(Lnet/minecraft/entity/LivingEntity;FFFFFF)V",
		require = 0,
		at = @At(
			value = "INVOKE",
			target = "Lorg/lwjgl/opengl/GL11;glTexEnv(IILjava/nio/FloatBuffer;)V",
			remap = false
		)
	)
	private void optic$hitColor(T entity, float a, float b, float c, float d, float e, float scale, CallbackInfo ci) {
		optic$rewriteHurtBuffer(entity);
	}

	@Inject(
		method = "a(Lpr;FZ)Z",
		remap = false,
		require = 0,
		at = @At(
			value = "INVOKE",
			target = "Lorg/lwjgl/opengl/GL11;glTexEnv(IILjava/nio/FloatBuffer;)V",
			remap = false
		)
	)
	private void optic$hitColorNotch(T entity, float tickDelta, boolean overlay, CallbackInfoReturnable<Boolean> cir) {
		optic$rewriteHurtBuffer(entity);
	}

	private void optic$rewriteHurtBuffer(T entity) {
		if (!OpticHitColor.enabled() || entity == null || buffer == null) return;
		if (entity.hurtTime <= 0 && entity.deathTime <= 0) return;
		try {
			buffer.position(0);
			if (buffer.remaining() < 4) {
				buffer.position(0);
				return;
			}
			float red = buffer.get();
			float green = buffer.get();
			float blue = buffer.get();
			buffer.get();
			if (red > 0.9F && green < 0.05F && blue < 0.05F) {
				buffer.clear();
				buffer.put(OpticHitColor.red());
				buffer.put(OpticHitColor.green());
				buffer.put(OpticHitColor.blue());
				buffer.put(OpticHitColor.alpha());
				buffer.flip();
			} else {
				buffer.position(0);
			}
		} catch (RuntimeException ignored) {
			buffer.position(0);
		}
	}

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

		if (gg.optic.client.render.OpticNametags.allHidden()) {
			cir.setReturnValue(false);
			return;
		}

		boolean hudHidden = client.options.hudHidden;
		if (hudHidden && c.nametagsHideInF1) {
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
			if (gg.optic.client.render.OpticNametags.playersHidden()) {
				cir.setReturnValue(false);
				return;
			}
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
		if (livingEntity instanceof PlayerEntity) {
			String suffix = OpticPing.nametagSuffix((PlayerEntity) livingEntity);
			if (suffix.length() > 0) text = text + suffix;
		}
		double yy = y - (livingEntity.isBaby() ? livingEntity.height / 2.0 : 0.0);
		OpticNametags.render(dispatcher, livingEntity, text, x, yy, z, 64);
	}
}
