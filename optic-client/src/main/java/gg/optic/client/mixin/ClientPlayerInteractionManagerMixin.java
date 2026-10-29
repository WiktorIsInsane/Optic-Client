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

	/**
	 * Vanilla waits for the server hurt packet before the target flashes.
	 * Instant Hit Flash shows it on the click instead. Off by default: the local
	 * flash also fires on hits the server ignores, which hides when the target's
	 * hurt time ends and makes combos hard to time. Health, motion, and the attack
	 * packet stay vanilla either way.
	 */
	@Inject(method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", at = @At("TAIL"))
	private void optic$instantHurt(PlayerEntity player, Entity target, CallbackInfo ci) {
		OpticSnap.onHit(player, target);
		if (!OpticClient.get().config().instantHitFlash) return;
		if (!(target instanceof LivingEntity) || target == player) return;
		LivingEntity living = (LivingEntity) target;
		living.maxHurtTime = 10;
		living.hurtTime = 10;
		double dx = player.x - living.x;
		double dz = player.z - living.z;
		living.knockbackVelocity = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI - living.yaw);
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
