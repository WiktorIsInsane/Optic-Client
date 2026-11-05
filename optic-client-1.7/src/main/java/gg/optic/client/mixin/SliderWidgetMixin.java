package gg.optic.client.mixin;

import gg.optic.client.gui.OpticButtonStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionSliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Slider handle drawn as a small box (vanilla draws two halves from the widgets texture). */
@Mixin(OptionSliderWidget.class)
public abstract class SliderWidgetMixin {
	@Redirect(
		method = "mouseDragged(Lnet/minecraft/client/MinecraftClient;II)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/OptionSliderWidget;drawTexture(IIIIII)V", ordinal = 0),
		require = 0
	)
	private void optic$knob(OptionSliderWidget self, int x, int y, int u, int v, int w, int h) {
		ButtonWidget button = (ButtonWidget) (Object) self;
		OpticButtonStyle.knob(MinecraftClient.getInstance(), x, y, 8, h, button.isHovered());
	}

	@Redirect(
		method = "mouseDragged(Lnet/minecraft/client/MinecraftClient;II)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/OptionSliderWidget;drawTexture(IIIIII)V", ordinal = 1),
		require = 0
	)
	private void optic$knobRight(OptionSliderWidget self, int x, int y, int u, int v, int w, int h) {
	}
}