package gg.optic.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A dead mob or player stays in the world for the fall-over animation and
 * {@code collides()} ignores health, so the corpse can still be hit.
 * Drop it on the client as soon as its health hits zero. The local player
 * stays so the death screen still has a camera.
 */
@Mixin(LivingEntity.class)
public class LivingEntityDeathMixin {
	@Inject(method = "tick()V", at = @At("HEAD"))
	private void optic$dropCorpse(CallbackInfo ci) {
		optic$vanishIfDead();
	}

	@Inject(method = "handleStatus(B)V", at = @At("RETURN"))
	private void optic$dropOnDeathStatus(byte status, CallbackInfo ci) {
		optic$vanishIfDead();
	}

	private void optic$vanishIfDead() {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity.getHealth() > 0.0F) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && entity == client.player) return;
		entity.removed = true;
	}
}
