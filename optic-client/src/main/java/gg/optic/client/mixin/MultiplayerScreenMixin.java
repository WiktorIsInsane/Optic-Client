package gg.optic.client.mixin;

import gg.optic.client.OpticUiFont;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.resource.language.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
	@Redirect(
		method = "render(IIF)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerScreen;drawCenteredString(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"
		)
	)
	private void optic$hideVanillaTitle(MultiplayerScreen self, TextRenderer renderer, String text, int x, int y, int color) {
	}

	@Inject(method = "render(IIF)V", at = @At("TAIL"))
	private void optic$chrome(int mouseX, int mouseY, float delta, CallbackInfo ci) {
		String title = I18n.translate("multiplayer.title");
		int color = 0xFFFFFFFF;
		float size = 11.0F;
		int textW = OpticUiFont.widthSharp(this.client, title, color, size);
		OpticUiFont.drawSharp(this.client, title, this.width / 2 - textW / 2, 6, color, size);
	}
}
