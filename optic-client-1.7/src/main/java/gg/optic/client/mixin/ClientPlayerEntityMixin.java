package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.render.OpticHitParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Your own hit particles with the Particles module on: instead of vanilla's emitter, spawn the
 * full burst right away times the Particle Multiplier (OpticHitParticles.burst).
 */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
	@Inject(method = "addCritParticles(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
	private void optic$ownCrit(Entity target, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.particles) return;
		ci.cancel();
		if (c.renderOwnParticles && c.showCriticalHitParticles) OpticHitParticles.burst(target, "crit");
	}

	@Inject(method = "addEnchantedHitParticles(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
	private void optic$ownSharp(Entity target, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.particles) return;
		ci.cancel();
		if (c.renderOwnParticles) OpticHitParticles.burst(target, "magicCrit");
	}
}
