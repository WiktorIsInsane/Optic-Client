package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.particle.DamageParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DamageParticle.class)
public abstract class DamageParticleMixin {
	@Inject(method = "<init>(Lnet/minecraft/world/World;DDDDDD)V", at = @At("TAIL"))
	private void optic$brightInit(World world, double d, double e, double f, double g, double h, double i, CallbackInfo ci) {
		optic$applyNoFilter((Particle) (Object) this);
	}

	@Inject(method = "<init>(Lnet/minecraft/world/World;DDDDDDF)V", at = @At("TAIL"))
	private void optic$brightInitScaled(World world, double d, double e, double f, double g, double h, double i, float j, CallbackInfo ci) {
		optic$applyNoFilter((Particle) (Object) this);
	}

	@Inject(method = "tick()V", at = @At("TAIL"))
	private void optic$keepBright(CallbackInfo ci) {
		optic$applyNoFilter((Particle) (Object) this);
	}

	private static void optic$applyNoFilter(Particle particle) {
		OpticConfig c = OpticClient.get().config();
		if (!c.particles || !c.removeVanillaColorFilter) return;
		particle.setColor(1.0F, 1.0F, 1.0F);
	}

	@Mixin(DamageParticle.CritMagicFactory.class)
	public static class CritMagicFactoryMixin {
		@Redirect(
			method = "createParticle(ILnet/minecraft/world/World;DDDDDD[I)Lnet/minecraft/client/particle/Particle;",
			at = @At(
				value = "INVOKE",
				target = "Lnet/minecraft/client/particle/Particle;setColor(FFF)V"
			)
		)
		private void optic$skipMagicTint(Particle particle, float r, float g, float b) {
			OpticConfig c = OpticClient.get().config();
			if (c.particles && c.removeVanillaColorFilter) {
				particle.setColor(1.0F, 1.0F, 1.0F);
			} else {
				particle.setColor(r, g, b);
			}
		}
	}
}
