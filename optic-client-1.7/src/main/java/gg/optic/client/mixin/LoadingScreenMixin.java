package gg.optic.client.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.gui.OpticMenuChrome;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.LoadingScreenRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.Window;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** "Loading world" / "Building terrain": the menu wallpaper instead of the dirt background. */
@Mixin(LoadingScreenRenderer.class)
public abstract class LoadingScreenMixin {
	@Redirect(
		method = "setProgressPercentage(I)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;end()I", ordinal = 0),
		require = 0
	)
	private int optic$wallpaper(Tessellator tessellator) {
		// The dirt quad is already started; finish it without drawing anything.
		GL11.glColorMask(false, false, false, false);
		int drawn = tessellator.end();
		GL11.glColorMask(true, true, true, true);
		MinecraftClient client = MinecraftClient.getInstance();
		Window window = new Window(client, client.width, client.height);
		int w = window.getWidth();
		int h = window.getHeight();
		GlStateManager.enableTexture();
		OpticMenuChrome.drawWallpaper(client, w, h);
		GlStateManager.enableBlend();
		DrawableHelper.fill(0, 0, w, h, 0x8C000000);
		GlStateManager.enableTexture();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		return drawn;
	}
}