package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ItemRenderer.class)
public class ItemRendererGlintMixin {
	@ModifyConstant(method = "renderGlint", constant = @Constant(longValue = 3000L))
	private long optic$oldGlintSpeedA(long original) {
		OpticConfig c = OpticClient.get().config();
		return c.anim(c.animOldEnchantGlint) ? 4500L : original;
	}

	@ModifyConstant(method = "renderGlint", constant = @Constant(longValue = 4873L))
	private long optic$oldGlintSpeedB(long original) {
		OpticConfig c = OpticClient.get().config();
		return c.anim(c.animOldEnchantGlint) ? 6500L : original;
	}

	@ModifyConstant(method = "renderGlint", constant = @Constant(floatValue = -50.0F))
	private float optic$oldGlintAngleA(float original) {
		OpticConfig c = OpticClient.get().config();
		return c.anim(c.animOldEnchantGlint) ? -30.0F : original;
	}

	@ModifyConstant(method = "renderGlint", constant = @Constant(floatValue = 10.0F))
	private float optic$oldGlintAngleB(float original) {
		OpticConfig c = OpticClient.get().config();
		return c.anim(c.animOldEnchantGlint) ? 20.0F : original;
	}
}
