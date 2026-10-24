package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.render.entity.model.BiPedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BiPedModel.class)
public class BiPedModelMixin {
	@ModifyConstant(method = "setAngles", constant = @Constant(floatValue = -0.5235988F))
	private float optic$oldBlockArm(float value) {
		OpticConfig c = OpticClient.get().config();
		return c.anim(c.animBlockHit) ? 0.0F : value;
	}
}
