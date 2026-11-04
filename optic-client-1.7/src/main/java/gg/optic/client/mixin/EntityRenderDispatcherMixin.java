package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.render.OpticHitboxes;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hitboxes module on 1.7.10: colored boxes with F3+B off, and recolored F3+B boxes. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Shadow public static boolean renderHitboxes;

	@Inject(method = "method_6913(Lnet/minecraft/entity/Entity;DDDFFZ)Z", at = @At("RETURN"))
	private void optic$drawModHitboxes(Entity entity, double x, double y, double z, float yaw, float tickDelta, boolean skipHitbox, CallbackInfoReturnable<Boolean> cir) {
		OpticConfig c = OpticClient.get().config();
		if (!c.hitboxes || skipHitbox || renderHitboxes) return;
		OpticHitboxes.draw(entity, x, y, z, tickDelta, c);
	}

	@Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
	private void optic$coloredHitboxes(Entity entity, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.hitboxes) return;
		ci.cancel();
		OpticHitboxes.draw(entity, x, y, z, tickDelta, c);
	}
}
