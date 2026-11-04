package gg.optic.client.mixin;

import gg.optic.client.render.OpticHitParticles;
import net.minecraft.client.particle.DamageParticle;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The hit emitter (crit / magicCrit) spawns 16 particles a tick for 3 ticks. With the Particles
 * module on, those go through OpticHitParticles so the 1.7 particle setting can't drop them and
 * the multiplier applies to every hit (your own, server animations, and Always Sharpness).
 */
@Mixin(DamageParticle.class)
public abstract class DamageParticleMixin {
	@Redirect(
		method = "tick()V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnParticle(Ljava/lang/String;DDDDDD)V"),
		require = 0
	)
	private void optic$hitParticle(World world, String name, double x, double y, double z, double vx, double vy, double vz) {
		if (OpticHitParticles.handles(name)) {
			OpticHitParticles.spawn(world, name, x, y, z, vx, vy, vz);
			return;
		}
		world.spawnParticle(name, x, y, z, vx, vy, vz);
	}
}
