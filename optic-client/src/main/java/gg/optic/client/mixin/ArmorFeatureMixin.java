package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorFeatureRenderer.class)
public class ArmorFeatureMixin {
	@Inject(method = "combineTextures()Z", at = @At("HEAD"), cancellable = true)
	private void optic$damageRedArmor(CallbackInfoReturnable<Boolean> cir) {
		OpticConfig c = OpticClient.get().config();
		if (c.anim(c.animDamage)) {
			cir.setReturnValue(true);
		}
	}
}
