package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.render.OpticBlockOverlay;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResultType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Block Overlay on 1.7.10. */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
	@Shadow private ClientWorld world;

	@Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
	private void optic$blockOverlay(PlayerEntity player, BlockHitResult hitResult, int pass, float tickDelta, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.blockOverlay) return;
		if (pass != 0 || hitResult == null || hitResult.hitResult != HitResultType.BLOCK) return;
		ci.cancel();
		OpticBlockOverlay.draw(this.world, player, hitResult, tickDelta, c);
	}
}
