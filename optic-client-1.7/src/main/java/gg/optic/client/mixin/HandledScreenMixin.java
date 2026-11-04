package gg.optic.client.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hotbar slot keys bound to mouse buttons (mouse4, mouse5, …) only fire from the keyboard
 * path in chests and inventories. Treat those clicks like the number keys.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
	@Shadow
	protected abstract boolean handleHotbarKeyPressed(int keyCode);

	@Inject(method = "mouseClicked(III)V", at = @At("HEAD"), cancellable = true)
	private void optic$mouseHotbarKeys(int mouseX, int mouseY, int button, CallbackInfo ci) {
		// 0 and 1 stay left/right slot clicks. Mouse buttons are stored as button - 100.
		if (button < 2) return;
		if (this.handleHotbarKeyPressed(button - 100)) {
			ci.cancel();
		}
	}
}
