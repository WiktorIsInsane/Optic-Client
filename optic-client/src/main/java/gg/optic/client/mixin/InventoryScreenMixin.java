package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
	@ModifyConstant(method = "applyStatusEffectOffset", constant = @Constant(intValue = 200))
	private int optic$oldInventoryOffset(int original) {
		OpticConfig c = OpticClient.get().config();
		return c.anim(c.animInventory) ? 324 : original;
	}
}
