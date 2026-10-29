package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.render.OpticHitPredict;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleType;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
	@Redirect(
		method = "onEntityAnimation(Lnet/minecraft/network/packet/s2c/play/EntityAnimationS2CPacket;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/particle/ParticleManager;addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/particle/ParticleType;)V"
		)
	)
	private void optic$filterHitParticles(ParticleManager manager, Entity entity, ParticleType type) {
		OpticConfig c = OpticClient.get().config();
		// Off hides both kinds of hit particles the server sends: plain and enchanted crits.
		if (c.particles && !c.showCriticalHitParticles && (type == ParticleType.CRIT || type == ParticleType.CRIT_MAGIC)) {
			return;
		}
		manager.addEmitter(entity, type);
	}

	@Inject(method = "onEntityStatus(Lnet/minecraft/network/packet/s2c/play/EntityStatusS2CPacket;)V", at = @At("TAIL"))
	private void optic$serverHurt(EntityStatusS2CPacket packet, CallbackInfo ci) {
		if (packet.getStatus() != 2) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return;
		OpticHitPredict.onServerHurt(packet.getEntity(client.world));
	}
}
