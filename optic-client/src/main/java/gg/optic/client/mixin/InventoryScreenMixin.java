package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.hud.OpticPotions;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
	@Inject(method = "drawStatusEffects()V", at = @At("HEAD"), cancellable = true)
	private void optic$hideVanillaPotions(CallbackInfo ci) {
		if (OpticPotions.hideVanilla()) ci.cancel();
	}

	@ModifyConstant(method = "applyStatusEffectOffset", constant = @Constant(intValue = 200))
	private int optic$oldInventoryOffset(int original) {
		OpticConfig c = OpticClient.get().config();
		return c.anim(c.animInventory) ? 324 : original;
	}
}
