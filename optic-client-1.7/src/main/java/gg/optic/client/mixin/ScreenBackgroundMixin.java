package gg.optic.client.mixin;

import gg.optic.client.gui.OpticWorldBackground;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** World and server screens show a darkened Minecraft world instead of dirt. */
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
	@Shadow
	protected MinecraftClient client;

	@Shadow
	public int width;

	@Shadow
	public int height;

	/**
	 * In a world, menus show the game behind them (vanilla's dark tint). Out of a world they show
	 * the menu wallpaper. "Downloading terrain" always gets the wallpaper: there is no game to show yet.
	 */
	private boolean optic$useWallpaper() {
		if (client == null) return false;
		if ((Object) this instanceof net.minecraft.client.gui.screen.TitleScreen) return false;
		if ((Object) this instanceof net.minecraft.client.gui.screen.DownloadingTerrainScreen) return true;
		return client.world == null;
	}

	@Inject(method = "renderBackground()V", at = @At("HEAD"), cancellable = true)
	private void optic$worldBackground(CallbackInfo ci) {
		if (!optic$useWallpaper()) return;
		OpticWorldBackground.draw(client, this.width, this.height);
		ci.cancel();
	}

	/** Screens that draw dirt directly: wallpaper at the menu, the game (tinted) when in a world. */
	@Inject(method = "renderDirtBackground(I)V", at = @At("HEAD"), cancellable = true)
	private void optic$packBackground(int alpha, CallbackInfo ci) {
		if (client == null || (Object) this instanceof net.minecraft.client.gui.screen.TitleScreen) return;
		if (optic$useWallpaper()) {
			OpticWorldBackground.draw(client, this.width, this.height);
		} else {
			((Screen) (Object) this).renderBackground();
		}
		ci.cancel();
	}
}