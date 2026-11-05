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

	/** About what vanilla's 3-tick hit emitter spawns (16 tries a tick, ~half kept). */
	private static final int BURST = 24;

	/**
	 * A whole hit burst at once: BURST particles times the multiplier, spread over the target
	 * the way vanilla's emitter does. Used for your own hits so the count follows the slider
	 * (vanilla's emitter only ticks a few times and was easy to lose).
	 */
	public static void burst(Entity target, String name) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (target == null || client == null || client.world == null) return;
		OpticConfig c = OpticClient.get().config();
		float m = Math.max(0.0F, Math.min(10.0F, c.particleMultiplier));
		int count = Math.round(BURST * m);
		if (!inRange(client, target.x, target.y, target.z)) return;
		double midY = target.boundingBox.minY + target.height / 2.0F;
		for (int i = 0; i < count; i++) {
			double vx;
			double vy;
			double vz;
			do {
				vx = RANDOM.nextFloat() * 2.0F - 1.0F;
				vy = RANDOM.nextFloat() * 2.0F - 1.0F;
				vz = RANDOM.nextFloat() * 2.0F - 1.0F;
			} while (vx * vx + vy * vy + vz * vz > 1.0);
			double x = target.x + vx * target.width / 4.0;
			double y = midY + vy * target.height / 4.0;
			double z = target.z + vz * target.width / 4.0;
			spawnOne(client, c, client.world, name, x, y, z, vx, vy + 0.2, vz);
		}
	}

	/** Spawns one vanilla-looking crit/magicCrit particle times the multiplier. */
	public static void spawn(World world, String name, double x, double y, double z, double vx, double vy, double vz) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.particleManager == null || world == null) return;
		if (!inRange(client, x, y, z)) return;
		OpticConfig c = OpticClient.get().config();
		int copies = copies(c.particleMultiplier);
		for (int i = 0; i < copies; i++) {
			double jx = i == 0 ? 0.0 : (RANDOM.nextDouble() - 0.5) * 0.15;
			double jy = i == 0 ? 0.0 : (RANDOM.nextDouble() - 0.5) * 0.15;
			double jz = i == 0 ? 0.0 : (RANDOM.nextDouble() - 0.5) * 0.15;
			spawnOne(client, c, world, name, x + jx, y + jy, z + jz, vx, vy, vz);
		}
	}

	private static boolean inRange(MinecraftClient client, double x, double y, double z) {
		Entity camera = client.field_6279;
		if (camera == null) return true;
		double dx = camera.x - x;
		double dy = camera.y - y;
		double dz = camera.z - z;
		return dx * dx + dy * dy + dz * dz <= 32.0 * 32.0;
	}

	private static void spawnOne(MinecraftClient client, OpticConfig c, World world, String name, double x, double y, double z, double vx, double vy, double vz) {
		if (client.particleManager == null) return;
		Particle particle = new LargeFireSmokeParticle(world, x, y, z, vx, vy, vz);
		if ("magicCrit".equals(name)) {
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
