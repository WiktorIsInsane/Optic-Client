package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
	@Inject(method = "addCritParticles(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
	private void optic$ownCrit(Entity target, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.particles) return;
		if (!c.renderOwnParticles || !c.showCriticalHitParticles) {
			ci.cancel();
		}
	}

	@Inject(method = "addEnchantedHitParticles(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
	private void optic$ownSharp(Entity target, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.particles) return;
		if (!c.renderOwnParticles) {
			ci.cancel();
		}
	}
}
