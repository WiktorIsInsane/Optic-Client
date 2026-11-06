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
	/**
	 * Toggle Sprint, the way 1.8's held sprint key works: sprint comes back right after this
	 * tick's keys are read and BEFORE you move. Turning it back on at the end of the tick (as
	 * before) cost a whole tick of unsprinted movement after every hit, so combos lost
	 * momentum. Vanilla's own stop checks (blocking, hunger, walls, not holding forward) still
	 * run after this.
	 */
	@Inject(
		method = "tickMovement()V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;tick()V", shift = At.Shift.AFTER),
		require = 0
	)
	private void optic$toggleSprintBeforeMove(CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.toggleSprint || !c.toggleSprintToggled) return;
		ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
		if (self.isSprinting() || self.input == null || self.input.movementForward < 0.8F) return;
		if (self.isSneaking() || self.horizontalCollision || self.isUsingItem()) return;
		if (self.hasStatusEffect(net.minecraft.entity.effect.StatusEffect.BLINDNESS)) return;
		if (self.getHungerManager().getFoodLevel() <= 6 && !self.abilities.allowFlying) return;
		self.setSprinting(true);
	}

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
		// As on 1.8.9: Always Sharpness keeps your own sharpness particles even with Render Own
		// Particles off (that combo hid them only on enchanted swords, while fists showed them).
		if (c.renderOwnParticles || c.alwaysSharpnessParticles) OpticHitParticles.burst(target, "magicCrit");
	}
}
