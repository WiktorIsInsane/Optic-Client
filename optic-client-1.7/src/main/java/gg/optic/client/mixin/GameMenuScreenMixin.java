package gg.optic.client.mixin;

import gg.optic.client.OpticUiDraw;
import gg.optic.client.gui.OpticCosmeticsScreen;
import gg.optic.client.gui.OpticMenuChrome;
import gg.optic.client.gui.OpticModScreen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pause menu: Optic labels, no card or logo; buttons use the shared transparent box style. */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
	@Inject(method = "init()V", at = @At("TAIL"))
	private void optic$labels(CallbackInfo ci) {
		for (Object entry : this.buttons) {
			if (!(entry instanceof ButtonWidget)) continue;
			ButtonWidget button = (ButtonWidget) entry;
			if (button.id == 1) button.message = "Exit";
			else if (button.id == 0) button.message = "Options";
			else if (button.id == 5) button.message = "Servers";
			else if (button.id == 6) button.message = "Cosmetics";
			else if (button.id == 7) {
				button.message = "Mods";
				button.active = true;
			}
		}
	}

	@Inject(method = "buttonClicked(Lnet/minecraft/client/gui/widget/ButtonWidget;)V", at = @At("HEAD"), cancellable = true)
	private void optic$actions(ButtonWidget button, CallbackInfo ci) {
		if (button.id == 5) {
			this.client.setScreen(new MultiplayerScreen((Screen) (Object) this));
			ci.cancel();
		} else if (button.id == 6) {
			this.client.setScreen(new OpticCosmeticsScreen((Screen) (Object) this));
			ci.cancel();
		} else if (button.id == 7) {
			this.client.setScreen(new OpticModScreen());
			ci.cancel();
		}
	}

	@Redirect(
		method = "render(IIF)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/GameMenuScreen;drawCenteredString(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"
		)
	)
	private void optic$hideTitle(GameMenuScreen self, TextRenderer renderer, String text, int x, int y, int color) {
	}
}
