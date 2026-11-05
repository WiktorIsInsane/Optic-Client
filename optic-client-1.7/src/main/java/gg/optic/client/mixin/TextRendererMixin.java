package gg.optic.client.mixin;

import gg.optic.client.render.OpticNickHider;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Nick Hider: every string the game draws or measures passes through here, so names change
 * everywhere at once (chat, tab, nametags, scoreboard, titles) and widths stay right.
 */
@Mixin(TextRenderer.class)
public abstract class TextRendererMixin {
	@ModifyVariable(method = "draw(Ljava/lang/String;IIIZ)I", at = @At("HEAD"), argsOnly = true)
	private String optic$nickDraw(String text) {
		return OpticNickHider.transform(text);
	}

	@ModifyVariable(method = "getStringWidth(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true)
	private String optic$nickWidth(String text) {
		return OpticNickHider.transform(text);
	}

	@ModifyVariable(method = "wrapLines(Ljava/lang/String;I)Ljava/util/List;", at = @At("HEAD"), argsOnly = true)
	private String optic$nickWrap(String text) {
		return OpticNickHider.transform(text);
	}

	@ModifyVariable(method = "trimToWidth(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("HEAD"), argsOnly = true)
	private String optic$nickTrim(String text) {
		return OpticNickHider.transform(text);
	}

	@ModifyVariable(method = "drawTrimmed(Ljava/lang/String;IIII)V", at = @At("HEAD"), argsOnly = true)
	private String optic$nickTrimmed(String text) {
		return OpticNickHider.transform(text);
	}
}
