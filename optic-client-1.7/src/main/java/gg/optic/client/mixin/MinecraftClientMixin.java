package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticGui;
import gg.optic.client.OpticZoom;
import gg.optic.client.display.OpticBorderless;
import gg.optic.client.gui.OpticHudEditorScreen;
import gg.optic.client.gui.OpticModScreen;
import gg.optic.client.hud.OpticHud;
import gg.optic.client.hud.OpticTab;
import gg.optic.client.input.OpticRawInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7.10 game-loop hooks. Starts Optic (there is no Fabric loader on this path), opens
 * the mod menu, and counts clicks for the CPS HUD. Combat is left exactly vanilla 1.7:
 * nothing here changes when or how a click becomes an attack.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Shadow public Screen currentScreen;
	@Shadow public GameOptions options;

	@Shadow public abstract void setScreen(Screen screen);

	@Unique private static boolean optic$started;
	@Unique private boolean optic$rightShiftDown;

	@Inject(method = "initializeGame()V", at = @At("TAIL"))
	private void optic$initialized(CallbackInfo ci) {
		if (!optic$started) {
			optic$started = true;
			OpticClient.get().onInitializeClient();
		}
		Display.setTitle("Optic 1.7.10");
		if (OpticClient.get().config().fullbright) {
			options.gamma = OpticClient.get().config().fullbrightGamma;
		}
		if (OpticClient.get().config().rawMouseInput) OpticRawInput.setEnabled(true);
		if (OpticClient.get().config().borderlessFullscreen) OpticBorderless.apply(true);
	}

	@Inject(method = "stop()V", at = @At("HEAD"))
	private void optic$saveOnExit(CallbackInfo ci) {
		OpticClient.get().config().save();
	}

	@Inject(method = "tick()V", at = @At("TAIL"))
	private void optic$tick(CallbackInfo ci) {
		boolean rightShift = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
		if (rightShift && !optic$rightShiftDown) {
			if (currentScreen instanceof OpticModScreen || currentScreen instanceof OpticHudEditorScreen) {
				setScreen(null);
			} else if (currentScreen == null) {
				setScreen(new OpticModScreen());
			}
		}
		optic$rightShiftDown = rightShift;
		MinecraftClient client = (MinecraftClient) (Object) this;
		OpticRawInput.tickClientThread();
		if (OpticClient.get().config().toggleSprint && options.forwardKey.isPressed()) {
			if (client.field_3805 != null && !client.field_3805.isSneaking()) {
				client.field_3805.setSprinting(true);
			}
		}
		OpticZoom.tick();
		OpticHud.tickCopy(client);
		OpticTab.tick(client);
	}

	/** CPS counter only. The click itself runs untouched. */
	@Inject(method = "doAttack()V", at = @At("HEAD"))
	private void optic$leftClick(CallbackInfo ci) {
		if (currentScreen == null && !OpticGui.isOpticScreenOpen()) OpticClient.get().clicks().click(0);
	}

	@Inject(method = "doUse()V", at = @At("HEAD"))
	private void optic$rightClick(CallbackInfo ci) {
		if (!OpticGui.isOpticScreenOpen()) OpticClient.get().clicks().click(1);
	}
}
