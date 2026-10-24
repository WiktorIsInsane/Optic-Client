package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.render.OpticNametags;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
	@Shadow @Final protected EntityRenderDispatcher dispatcher;

	@Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
	private void optic$nametag(T entity, String text, double x, double y, double z, int maxDistance, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.nametags) return;
		ci.cancel();
		OpticNametags.render(this.dispatcher, entity, text, x, y, z, maxDistance);
	}
}
