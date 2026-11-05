package gg.optic.client.mixin;

import gg.optic.client.cosmetics.OpticCosmetics;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optic cosmetics on your own player. Drawn after the held items and armor extras, where the
 * matrix is still in player model space. Drawn here instead of through vanilla's cape path,
 * so OptiFine's "Show Capes: off" and the hide-cape option don't hide them.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Inject(method = "method_1582(Lnet/minecraft/client/network/AbstractClientPlayerEntity;F)V", at = @At("TAIL"))
	private void optic$cosmetics(AbstractClientPlayerEntity player, float tickDelta, CallbackInfo ci) {
		if (!OpticCosmetics.isLocal(player) || player.isInvisible()) return;
		if (OpticCosmetics.capeEquipped()) OpticCosmetics.renderCape(player, tickDelta);
		if (OpticCosmetics.wingsEquipped()) OpticCosmetics.renderWings(player.isSneaking());
	}
}
