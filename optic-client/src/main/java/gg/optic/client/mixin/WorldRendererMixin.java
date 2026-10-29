package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticEnvironment;
import gg.optic.client.render.OpticBlockOverlay;
import gg.optic.client.render.OpticEntityCull;
import gg.optic.client.render.OpticHitPredict;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
	@Shadow private ClientWorld world;
	@Shadow @Final private MinecraftClient client;

	@Shadow
	protected abstract void renderEndSky();

	@Inject(method = "renderEntities", at = @At("HEAD"))
	private void optic$cullFrame(Entity camera, net.minecraft.client.render.CameraView view, float tickDelta, CallbackInfo ci) {
		OpticEntityCull.beginFrame(camera, tickDelta);
	}

	@Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
	private void optic$blockOverlay(PlayerEntity player, BlockHitResult hitResult, int pass, float tickDelta, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.blockOverlay) return;
		if (pass != 0 || hitResult == null || hitResult.type != BlockHitResult.Type.BLOCK) return;
		ci.cancel();
		OpticBlockOverlay.draw(this.world, player, hitResult, tickDelta, c);
	}

	@Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
	private void optic$sky(float tickDelta, int anaglyphFilter, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.timeChanger || this.world == null || this.world.dimension == null) return;
		// Only override overworld sky.
		if (this.world.dimension.getType() == 1 || !this.world.dimension.canPlayersSleep()) return;

		if (c.skyType == OpticEnvironment.SKY_END) {
			ci.cancel();
			this.renderEndSky();
		} else if (c.skyType == OpticEnvironment.SKY_NETHER) {
			ci.cancel();
		}
	}

	@Inject(method = "renderWorldBorder", at = @At("RETURN"))
	private void optic$hitPredict(Entity camera, float tickDelta, CallbackInfo ci) {
		OpticHitPredict.update(this.client, tickDelta);
		OpticHitPredict.draw(camera, tickDelta);
	}
}
