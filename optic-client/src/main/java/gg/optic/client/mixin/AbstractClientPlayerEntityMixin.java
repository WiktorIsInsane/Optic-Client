package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {
	@Inject(method = "getModel()Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
	private void optic$forceClassicSkin(CallbackInfoReturnable<String> cir) {
		OpticConfig c = OpticClient.get().config();
		// When 1.8 Skins is OFF, force classic Steve model (1.7-style).
		if (c.animations && !c.anim18Skins) {
			cir.setReturnValue("default");
		}
	}
}
