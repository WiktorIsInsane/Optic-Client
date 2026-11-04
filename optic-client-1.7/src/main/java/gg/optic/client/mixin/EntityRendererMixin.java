package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.hud.OpticPing;
import gg.optic.client.render.OpticNametags;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nametags module and nametag ping on 1.7.10. Entity rendering itself stays vanilla. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@Shadow protected EntityRenderDispatcher dispatcher;

	@ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private String optic$pingOnNametag(String text, Entity entity) {
		return optic$withPing(entity, text);
	}

	@Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
	private void optic$nametag(Entity entity, String text, double x, double y, double z, int maxDistance, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.nametags) return;
		ci.cancel();
		OpticNametags.render(this.dispatcher, entity, optic$withPing(entity, text), x, y, z, maxDistance);
	}

	@Unique
	private String optic$withPing(Entity entity, String text) {
		if (!(entity instanceof PlayerEntity) || text == null) return text;
		String suffix = OpticPing.nametagSuffix((PlayerEntity) entity);
		if (suffix.length() == 0 || text.endsWith(suffix)) return text;
		return text + suffix;
	}
}
