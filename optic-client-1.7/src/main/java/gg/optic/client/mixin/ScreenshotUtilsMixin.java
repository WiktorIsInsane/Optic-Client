package gg.optic.client.mixin;

import gg.optic.client.OpticScreenshots;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.ScreenshotUtils;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

/** Adds [Open] [Copy] [Folder] to the "Saved screenshot as ..." chat line. */
@Mixin(ScreenshotUtils.class)
public abstract class ScreenshotUtilsMixin {
	@Inject(
		method = "saveScreenshot(Ljava/io/File;Ljava/lang/String;IILnet/minecraft/client/gl/Framebuffer;)Lnet/minecraft/text/Text;",
		at = @At("RETURN"),
		cancellable = true,
		require = 0
	)
	private static void optic$buttons(File dir, String name, int width, int height, Framebuffer framebuffer, CallbackInfoReturnable<Text> cir) {
		cir.setReturnValue(OpticScreenshots.decorate(cir.getReturnValue()));
	}
}
