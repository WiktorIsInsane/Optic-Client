package gg.optic.client.mixin;

import gg.optic.client.cosmetics.OpticCosmetics;
import gg.optic.client.render.OpticNickHider;
import net.minecraft.util.Identifier;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** With the Optic cape on, vanilla's own cape stays off so the two don't overlap. */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
	/** Nick Hider: Steve instead of the real skin (own and/or others', per settings). */
	@Inject(method = "getSkinId()Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
	private void optic$nickSkin(CallbackInfoReturnable<Identifier> cir) {
		if (OpticNickHider.hideSkin((AbstractClientPlayerEntity) (Object) this)) {
			cir.setReturnValue(AbstractClientPlayerEntity.STEVE_TEXTURE);
		}
	}

	@Inject(method = "canRenderCapeTexture()Z", at = @At("HEAD"), cancellable = true)
	private void optic$hideVanillaCape(CallbackInfoReturnable<Boolean> cir) {
		if (OpticCosmetics.isLocal((PlayerEntity) (Object) this) && OpticCosmetics.capeEquipped()) {
			cir.setReturnValue(false);
		}
	}
}
