package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Particles module on 1.7.10. The server's hit animation spawns a crit or a "magicCrit"
 * (sharpness) emitter here; Show Critical Hit Particles off hides both.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
	/** Graphs: the server sends its world time every second; how far it moved gives TPS. */
	@org.spongepowered.asm.mixin.injection.Inject(
		method = "onWorldTimeUpdate(Lnet/minecraft/network/packet/s2c/play/WorldTimeUpdateS2CPacket;)V",
		at = @At("HEAD"),
		require = 0
	)
	private void optic$tps(net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket packet, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		gg.optic.client.hud.OpticGraphs.onWorldTime(packet.getTime());
	}

	@Redirect(
		method = "onEntityAnimation(Lnet/minecraft/network/packet/s2c/play/EntityAnimationS2CPacket;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;addParticle(Lnet/minecraft/client/particle/Particle;)V")
	)
	private void optic$filterHitParticles(ParticleManager manager, Particle particle) {
		OpticConfig c = OpticClient.get().config();
		if (c.particles && !c.showCriticalHitParticles) return;
		manager.addParticle(particle);
	}
}
