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

	@Inject(method = "renderBackground()V", at = @At("HEAD"), cancellable = true)
	private void optic$worldBackground(CallbackInfo ci) {
		if (!((Object) this instanceof MultiplayerScreen) && !((Object) this instanceof SelectWorldScreen)) return;
		OpticWorldBackground.draw(client, this.width, this.height);
		ci.cancel();
	}
}
