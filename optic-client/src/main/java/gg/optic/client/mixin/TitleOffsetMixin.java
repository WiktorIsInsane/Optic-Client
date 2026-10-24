package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InGameHud.class)
public class TitleOffsetMixin {
	@Shadow
	private String title;

	@Shadow
	private String subtitle;

	@Redirect(
		method = "render(F)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;FFIZ)I"
		)
	)
	private int optic$moveTitles(TextRenderer renderer, String text, float x, float y, int color, boolean shadow) {
		OpticConfig c = OpticClient.get().config();
		// The switch stores true as "1.7". Movable titles are the 1.8 side (false).
		if (c.animations && !c.animMovableTitles && text != null && (text.equals(this.title) || text.equals(this.subtitle))) {
			x += c.titleOffsetX;
			y += c.titleOffsetY;
		}
		return renderer.draw(text, x, y, color, shadow);
	}
}
