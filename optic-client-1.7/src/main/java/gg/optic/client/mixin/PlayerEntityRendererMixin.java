package gg.optic.client.mixin;

import gg.optic.client.cosmetics.OpticCosmetics;
import gg.optic.client.render.OpticSlimArms;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BiPedModel;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
	/** The player body model (armor models are separate fields and stay as they are). */
	@Shadow private BiPedModel field_2133;

	/** Slim-arm skins get 3px arms. Swapped per player, since one model draws everyone. */
	@Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;DDDFF)V", at = @At("HEAD"))
	private void optic$slimArms(AbstractClientPlayerEntity player, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
		OpticSlimArms.apply(this.field_2133, OpticSlimArms.isSlim(player));
	}

	/** Same for the first-person hand. */
	@Inject(method = "method_4339(Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"))
	private void optic$slimHand(PlayerEntity player, CallbackInfo ci) {
		boolean slim = player instanceof AbstractClientPlayerEntity && OpticSlimArms.isSlim((AbstractClientPlayerEntity) player);
		OpticSlimArms.apply(this.field_2133, slim);
	}

	@Inject(method = "method_1582(Lnet/minecraft/client/network/AbstractClientPlayerEntity;F)V", at = @At("TAIL"))
	private void optic$cosmetics(AbstractClientPlayerEntity player, float tickDelta, CallbackInfo ci) {
		if (!OpticCosmetics.isLocal(player) || player.isInvisible()) return;
		if (OpticCosmetics.capeEquipped()) OpticCosmetics.renderCape(player, tickDelta);
		if (OpticCosmetics.wingsEquipped()) OpticCosmetics.renderWings(player.isSneaking());
	}
}
