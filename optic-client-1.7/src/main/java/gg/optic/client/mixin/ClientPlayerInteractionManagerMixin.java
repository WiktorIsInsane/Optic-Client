package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Always Sharpness Particles on 1.7.10. Adds a local magicCrit emitter on your own hits
 * when the weapon has no sharpness. Client-side only: the attack packet already went out
 * from vanilla and nothing here is sent.
 */
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
	@Shadow @Final private MinecraftClient client;

	@Inject(method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", at = @At("TAIL"))
	private void optic$alwaysSharpness(PlayerEntity player, Entity target, CallbackInfo ci) {
		// Hit Predict: instant hurt flash (visual only).
		gg.optic.client.render.OpticHitPredict.onAttack(player, target);
		OpticConfig c = OpticClient.get().config();
		float modifier = target instanceof LivingEntity ? EnchantmentHelper.method_6387(player, (LivingEntity) target) : 0.0F;
		if (!c.particles || !c.alwaysSharpnessParticles) return;
		if (target == null || this.client.world == null || this.client.particleManager == null) return;
		// Vanilla already shows these when the weapon has sharpness (or smite/bane on the right mob).
		if (modifier > 0.0F) return;
		gg.optic.client.render.OpticHitParticles.burst(target, "magicCrit");
	}
}
