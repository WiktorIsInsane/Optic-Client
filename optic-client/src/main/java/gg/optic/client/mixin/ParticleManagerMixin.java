package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticOwnHit;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
	@Unique
	private static boolean optic$multiplying;

	/**
	 * Crit bursts (local swing and the server animation) are created here.
	 * Extra copies stay on the client. Nothing in this method sends a packet.
	 */
	@Inject(
		method = "addParticle(IDDDDDD[I)Lnet/minecraft/client/particle/Particle;",
		at = @At("HEAD"),
		cancellable = true
	)
	private void optic$critOptions(
		int id,
		double x,
		double y,
		double z,
		double velocityX,
		double velocityY,
		double velocityZ,
		int[] args,
		CallbackInfoReturnable<Particle> cir
	) {
		if (optic$multiplying) return;

		OpticConfig c = OpticClient.get().config();
		if (!c.particles) return;

		ParticleType type = ParticleType.getById(id);
		if (type != ParticleType.CRIT && type != ParticleType.CRIT_MAGIC) return;

		if (type == ParticleType.CRIT && !c.showCriticalHitParticles) {
			cir.setReturnValue(null);
			return;
		}
		boolean ownHit = OpticOwnHit.near(x, y, z);
		if (!c.renderOwnParticles && ownHit && !(type == ParticleType.CRIT_MAGIC && c.alwaysSharpnessParticles)) {
			cir.setReturnValue(null);
			return;
		}

		int copies = Math.round(c.particleMultiplier);
		if (copies < 0) copies = 0;
		if (copies > 10) copies = 10;
		if (copies <= 0) {
			cir.setReturnValue(null);
			return;
		}
		if (copies == 1) return;

		cir.setReturnValue(null);
		optic$multiplying = true;
		try {
			ParticleManager self = (ParticleManager) (Object) this;
			int[] safeArgs = args == null ? new int[0] : args;
			for (int i = 0; i < copies; i++) {
				double ox = (Math.random() - 0.5) * 0.12;
				double oy = (Math.random() - 0.5) * 0.12;
				double oz = (Math.random() - 0.5) * 0.12;
				self.addParticle(id, x + ox, y + oy, z + oz, velocityX, velocityY, velocityZ, safeArgs);
			}
		} finally {
			optic$multiplying = false;
		}
	}

	@Inject(
		method = "addParticle(IDDDDDD[I)Lnet/minecraft/client/particle/Particle;",
		at = @At("RETURN")
	)
	private void optic$whiteCrits(
		int id,
		double x,
		double y,
		double z,
		double velocityX,
		double velocityY,
		double velocityZ,
		int[] args,
		CallbackInfoReturnable<Particle> cir
	) {
		Particle particle = cir.getReturnValue();
		if (particle == null) return;
		OpticConfig c = OpticClient.get().config();
		if (!c.particles || !c.removeVanillaColorFilter) return;
		ParticleType type = ParticleType.getById(id);
		if (type != ParticleType.CRIT && type != ParticleType.CRIT_MAGIC) return;
		particle.setColor(1.0F, 1.0F, 1.0F);
	}

	@Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
	private void optic$blockBreak(BlockPos pos, BlockState state, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (c.particles && !c.blockBreakParticles) {
			ci.cancel();
		}
	}

	@Inject(method = "addBlockBreakingParticles", at = @At("HEAD"), cancellable = true)
	private void optic$blockBreaking(BlockPos pos, Direction direction, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (c.particles && !c.blockBreakParticles) {
			ci.cancel();
		}
	}
}
