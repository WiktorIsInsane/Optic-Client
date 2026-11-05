package gg.optic.client.mixin;

import gg.optic.client.cosmetics.OpticCosmetics;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** With the Optic cape on, vanilla's own cape stays off so the two don't overlap. */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
	@Inject(method = "canRenderCapeTexture()Z", at = @At("HEAD"), cancellable = true)
	private void optic$hideVanillaCape(CallbackInfoReturnable<Boolean> cir) {
		if (OpticCosmetics.isLocal((PlayerEntity) (Object) this) && OpticCosmetics.capeEquipped()) {
			cir.setReturnValue(false);
		}
	}
}
