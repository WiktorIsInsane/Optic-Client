package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.block.Block;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Particles module, Block Break Particles (on by default). Turned off, blocks give no particles
 * when broken (method_1294) or while you hit / mine them (method_1293). Visual only.
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
	@Inject(method = "method_1294(IIILnet/minecraft/block/Block;I)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void optic$blockBreak(int x, int y, int z, Block block, int meta, CallbackInfo ci) {
		if (optic$hideBlockParticles()) ci.cancel();
	}

	@Inject(method = "method_1293(IIII)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void optic$blockHit(int x, int y, int z, int side, CallbackInfo ci) {
		if (optic$hideBlockParticles()) ci.cancel();
	}

	private static boolean optic$hideBlockParticles() {
		OpticConfig c = OpticClient.get().config();
		return c.particles && !c.blockBreakParticles;
	}
}
