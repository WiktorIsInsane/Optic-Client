package gg.optic.client.render;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.LargeFireSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Hit particles for the Particles module on 1.7.10.
 *
 * Vanilla 1.7 routes "crit"/"magicCrit" through WorldRenderer.spawnParticleInternal, which
 * randomly drops a third of them on Decreased particles and drops all of them on Minimal.
 * With the module on, hit particles are built here instead so every hit shows them, and the
 * Particle Multiplier is applied. Purely visual and local.
 */
public final class OpticHitParticles {
	private static final Random RANDOM = new Random();

	private OpticHitParticles() {
	}

	public static boolean isHitParticle(String name) {
		return "crit".equals(name) || "magicCrit".equals(name);
	}

	/** True when the module should take over this particle from vanilla. */
	public static boolean handles(String name) {
		OpticConfig c = OpticClient.get().config();
		return c.particles && isHitParticle(name);
	}

	/** Spawns one vanilla-looking crit/magicCrit particle times the multiplier. */
	public static void spawn(World world, String name, double x, double y, double z, double vx, double vy, double vz) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.particleManager == null || world == null) return;
		Entity camera = client.field_6279;
		if (camera != null) {
			double dx = camera.x - x;
			double dy = camera.y - y;
			double dz = camera.z - z;
			if (dx * dx + dy * dy + dz * dz > 32.0 * 32.0) return;
		}
		OpticConfig c = OpticClient.get().config();
		int copies = copies(c.particleMultiplier);
		boolean magic = "magicCrit".equals(name);
		for (int i = 0; i < copies; i++) {
			double jx = i == 0 ? 0.0 : (RANDOM.nextDouble() - 0.5) * 0.15;
			double jy = i == 0 ? 0.0 : (RANDOM.nextDouble() - 0.5) * 0.15;
			double jz = i == 0 ? 0.0 : (RANDOM.nextDouble() - 0.5) * 0.15;
			Particle particle = new LargeFireSmokeParticle(world, x + jx, y + jy, z + jz, vx, vy, vz);
			if (magic) {
				if (c.removeVanillaColorFilter) {
					particle.setColor(1.0F, 1.0F, 1.0F);
				} else {
					particle.setColor(particle.getRed() * 0.3F, particle.getGreen() * 0.8F, particle.getBlue());
				}
				particle.method_5133();
			} else if (c.removeVanillaColorFilter) {
				particle.setColor(1.0F, 1.0F, 1.0F);
			}
			client.particleManager.addParticle(particle);
		}
	}

	/** 1.5 -> one copy plus a 50% chance of a second. 0 hides them. */
	private static int copies(float multiplier) {
		if (multiplier <= 0.0F) return 0;
		float m = Math.min(10.0F, multiplier);
		int whole = (int) m;
		float frac = m - whole;
		if (frac > 0.0F && RANDOM.nextFloat() < frac) whole++;
		return whole;
	}
}
