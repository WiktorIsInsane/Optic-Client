package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
	/**
	 * 1.8 sinks other players by an extra 0.125 while they sneak.
	 * The 1.7 sneaking option skips that drop. It does not change the collision box.
	 */
	@Redirect(
		method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;DDDFF)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isSneaking()Z"
		)
	)
	private boolean optic$oldSneak(AbstractClientPlayerEntity player) {
		OpticConfig c = OpticClient.get().config();
		if (c.anim(c.animSneaking)) return false;
		return player.isSneaking();
	}
}
