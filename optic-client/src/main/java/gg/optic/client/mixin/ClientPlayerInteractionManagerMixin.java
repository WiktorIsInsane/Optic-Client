package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticOwnHit;
import gg.optic.client.render.OpticSnap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.particle.ParticleType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
	@Shadow
	@Final
	private MinecraftClient client;

	@Inject(method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"))
	private void optic$rememberHit(PlayerEntity player, Entity target, CallbackInfo ci) {
		if (target == null) return;
		OpticOwnHit.mark(target.x, target.y + target.height * 0.5D, target.z);
	}

	/** The red hurt flash comes only from the server's hurt packet, as in vanilla. */
	@Inject(method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", at = @At("TAIL"))
	private void optic$snapOnHit(PlayerEntity player, Entity target, CallbackInfo ci) {
		OpticSnap.onHit(player, target);
	}

	/** Local particle emitter only. The attack packet was already sent by vanilla and is not changed here. */
	@Inject(method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", at = @At("TAIL"))
	private void optic$alwaysSharpness(PlayerEntity player, Entity target, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.particles || !c.alwaysSharpnessParticles) return;
		if (this.client.particleManager == null || target == null) return;

		float modifier;
		if (target instanceof LivingEntity) {
			modifier = EnchantmentHelper.getAttackDamage(player.getStackInHand(), ((LivingEntity) target).getGroup());
		} else {
			modifier = EnchantmentHelper.getAttackDamage(player.getStackInHand(), EntityGroup.DEFAULT);
		}

		// Vanilla already emits sharpness particles when modifier > 0.
		if (modifier <= 0.0F) {
			this.client.particleManager.addEmitter(target, ParticleType.CRIT_MAGIC);
		}
	}
}
