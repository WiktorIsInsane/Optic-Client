package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticGui;
import gg.optic.client.OpticNet;
import gg.optic.client.OpticZoom;
import gg.optic.client.display.OpticBorderless;
import gg.optic.client.gui.OpticHudEditorScreen;
import gg.optic.client.gui.OpticModScreen;
import gg.optic.client.hud.OpticGuiScale;
import gg.optic.client.hud.OpticHud;
import gg.optic.client.hud.OpticTab;
import gg.optic.client.input.OpticRawInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.network.ClientConnection;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

	/**
	 * Fullscreen destroys and recreates the LWJGL window. Release the raw-input hook on the
	 * old window first; the next tick attaches it to the new one. Leaving it on a dead window
	 * froze the game the first time it went fullscreen.
	 */
	@Inject(method = "toggleFullscreen()V", at = @At("HEAD"))
	private void optic$rawMouseBeforeFullscreen(CallbackInfo ci) {
		OpticRawInput.onDisplayChanged();
	}

	@Inject(method = "toggleFullscreen()V", at = @At("RETURN"))
	private void optic$rawMouseAfterFullscreen(CallbackInfo ci) {
		OpticRawInput.onDisplayChanged();
	}

	/** Zoom scroll and Disable Hotbar Scroll read the wheel before the hotbar does. */
	@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
	private int optic$scrollWheel() {
		int wheel = Mouse.getEventDWheel();
		if (OpticZoom.handleScroll(wheel)) return 0;
		if (OpticClient.get().config().disableMouseScrollWheel && currentScreen == null) return 0;
		return wheel;
	}

	/** GUI Scale module: per-screen scale for inventories and menus. */
	@Inject(method = "setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
	private void optic$guiScale(Screen screen, CallbackInfo ci) {
		OpticGuiScale.beforeScreen((MinecraftClient) (Object) this, screen);
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
		boolean itemInfo = OpticClient.get().config().itemInfo;
		if (options.heldItemTooltips != itemInfo) options.heldItemTooltips = itemInfo;
		if (OpticClient.get().config().toggleSprint && options.forwardKey.isPressed()) {
			if (client.field_3805 != null && !client.field_3805.isSneaking()) {
				client.field_3805.setSprinting(true);
			}
		}
		OpticZoom.tick();
		OpticHud.tickCopy(client);
		OpticTab.tick(client);
		// The tick's look, position and actions are written by now. Push them out immediately.
		OpticNet.flush(client);
	}

	/**
	 * 1.7 applies server packets once per tick, so hit flashes, knockback and other
	 * players' moves could sit for up to 50 ms. 1.8 applies them every frame; do the same.
	 * The in-game handler's per-tick hook is empty, and nothing extra is sent.
	 */
	@Inject(method = "runGameLoop()V", at = @At("HEAD"))
	private void optic$packetsEveryFrame(CallbackInfo ci) {
		MinecraftClient client = (MinecraftClient) (Object) this;
		if (client.world == null || client.getNetworkHandler() == null) return;
		ClientConnection connection = client.getNetworkHandler().getClientConnection();
		if (connection != null && connection.isOpen()) connection.tick();
	}

	/** CPS counter only. The click itself runs untouched. */
	@Inject(method = "doAttack()V", at = @At("HEAD"))
	private void optic$leftClick(CallbackInfo ci) {
		if (currentScreen == null && !OpticGui.isOpticScreenOpen()) OpticClient.get().clicks().click(0);
	}

	/** The swing and attack packets are queued by now; send them without waiting. */
	@Inject(method = "doAttack()V", at = @At("RETURN"))
	private void optic$sendAttackNow(CallbackInfo ci) {
		OpticNet.flush((MinecraftClient) (Object) this);
	}

	@Inject(method = "doUse()V", at = @At("HEAD"))
	private void optic$rightClick(CallbackInfo ci) {
		if (!OpticGui.isOpticScreenOpen()) OpticClient.get().clicks().click(1);
	}
}
