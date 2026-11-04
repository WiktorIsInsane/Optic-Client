package gg.optic.client.mixin;

import gg.optic.client.render.OpticSkins3D;
import net.minecraft.client.texture.PlayerSkinTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.image.BufferedImage;

/** Attaches the uncropped skin to its texture when the parsed image is handed over. */
@Mixin(PlayerSkinTexture.class)
public abstract class PlayerSkinTextureMixin implements OpticSkins3D.RawSkin {
	@Unique
	private volatile BufferedImage optic$raw;

	@Inject(method = "method_6994(Ljava/awt/image/BufferedImage;)V", at = @At("HEAD"), require = 0)
	private void optic$takeRaw(BufferedImage parsed, CallbackInfo ci) {
		BufferedImage raw = OpticSkins3D.takeRaw(parsed);
		if (raw != null) this.optic$raw = raw;
	}

	@Override
	public BufferedImage optic$rawSkin() {
		return this.optic$raw;
	}

	@Override
	public void optic$setRawSkin(BufferedImage image) {
		this.optic$raw = image;
	}
}
