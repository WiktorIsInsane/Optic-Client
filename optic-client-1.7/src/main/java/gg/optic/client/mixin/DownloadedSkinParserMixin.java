package gg.optic.client.mixin;

import gg.optic.client.render.OpticSkins3D;
import net.minecraft.client.render.DownloadedSkinParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.image.BufferedImage;

/** Keeps the uncropped skin (1.7 cuts it to 64x32) so 3D Skins can read the 64x64 layers. */
@Mixin(DownloadedSkinParser.class)
public abstract class DownloadedSkinParserMixin {
	@Inject(method = "parseSkin", at = @At("RETURN"), require = 0)
	private void optic$keepRaw(BufferedImage source, CallbackInfoReturnable<BufferedImage> cir) {
		OpticSkins3D.rememberRaw(cir.getReturnValue(), source);
	}
}
