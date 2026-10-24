package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.render.OpticHitboxes;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	/** Draw colored hitboxes when the mod is on and F3+B is off. */
	@Inject(method = "method_6913(Lnet/minecraft/entity/Entity;DDDFFZ)Z", at = @At("RETURN"))
	private void optic$drawModHitboxes(
		Entity entity,
		double x,
		double y,
		double z,
		float yaw,
		float tickDelta,
		boolean skipHitbox,
		CallbackInfoReturnable<Boolean> cir
	) {
		OpticConfig c = OpticClient.get().config();
		if (!c.hitboxes || skipHitbox) return;
		if (((EntityRenderDispatcher) (Object) this).getRenderHitboxes()) return;
		OpticHitboxes.draw(entity, x, y, z, tickDelta, c);
	}

	/** Recolor the vanilla F3+B pass while the mod is enabled. */
	@Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
	private void optic$coloredHitboxes(Entity entity, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.hitboxes) return;
		ci.cancel();
		OpticHitboxes.draw(entity, x, y, z, tickDelta, c);
	}

	/** 1.7 sneak outline when the hitbox mod is off, so the animation option is still visible. */
	@Inject(method = "renderHitbox", at = @At("RETURN"))
	private void optic$oldSneakHitbox(Entity entity, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (c.hitboxes || !c.anim(c.animHitbox)) return;
		if (!(entity instanceof PlayerEntity) || !entity.isSneaking()) return;
		OpticHitboxes.draw(entity, x, y, z, tickDelta, c);
	}
}
