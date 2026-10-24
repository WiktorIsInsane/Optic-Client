package gg.optic.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.LanguageButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The title screen draws its own globe. Keep the button for the language screen click. */
@Mixin(LanguageButton.class)
public class LanguageButtonMixin {
	@Inject(method = "render(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("HEAD"), cancellable = true)
	private void optic$hideVanillaGlobe(MinecraftClient client, int mouseX, int mouseY, CallbackInfo ci) {
		if (client != null && client.currentScreen instanceof TitleScreen) {
			ci.cancel();
		}
	}
}
