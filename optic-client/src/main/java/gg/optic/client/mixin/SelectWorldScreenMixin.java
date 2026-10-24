package gg.optic.client.mixin;

import gg.optic.client.OpticUiFont;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin extends Screen {
	@Shadow
	protected String title;

	@Redirect(
		method = "render(IIF)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/world/SelectWorldScreen;drawCenteredString(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"
		)
	)
	private void optic$hideVanillaTitle(SelectWorldScreen self, TextRenderer renderer, String text, int x, int y, int color) {
	}

	@Inject(method = "render(IIF)V", at = @At("TAIL"))
	private void optic$title(int mouseX, int mouseY, float delta, CallbackInfo ci) {
		String label = title == null ? "" : title;
		int color = 0xFFFFFFFF;
		float size = 11.0F;
		int textW = OpticUiFont.widthSharp(this.client, label, color, size);
		OpticUiFont.drawSharp(this.client, label, this.width / 2 - textW / 2, 6, color, size);
	}
}
