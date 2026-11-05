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
public abstract class MinecraftClientMixin implements gg.optic.client.account.SessionSwap.SessionHolder {
	/** Account switching swaps this; it is final in vanilla. */
	@Shadow @org.spongepowered.asm.mixin.Mutable @org.spongepowered.asm.mixin.Final private net.minecraft.client.util.Session session;

	@Override
	public void optic$setSession(net.minecraft.client.util.Session session) {
		this.session = session;
	}

	@Shadow public Screen currentScreen;
	@Shadow public GameOptions options;

	@Shadow public abstract void setScreen(Screen screen);

	@Shadow private boolean fullscreen;

	@Shadow
	private void onResolutionChanged(int width, int height) {
	}

	@Shadow
	private void resizeFramebuffer() {
	}

	/**
	 * Windowed mode: keep the game exactly the size of the window. 1.7 resizes its buffer
	 * before leaving fullscreen and only checks for drag-resizes, so the window could end up a
	 * different size than the picture (or larger than the screen) and parts were cut off.
	 */
	@Unique
	private void optic$fitWindow(MinecraftClient client) {
		if (fullscreen || OpticBorderless.isApplied() || !Display.isCreated()) return;
		try {
			org.lwjgl.opengl.DisplayMode desktop = Display.getDesktopDisplayMode();
			int w = Display.getWidth();
			int h = Display.getHeight();
			if (desktop != null && (w > desktop.getWidth() || h > desktop.getHeight() - 60)) {
				int fw = Math.round(desktop.getWidth() * 0.85F);
				int fh = Math.round(desktop.getHeight() * 0.85F);
				Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(fw, fh));
				Display.setLocation((desktop.getWidth() - fw) / 2, (desktop.getHeight() - fh) / 2);
				w = Display.getWidth();
				h = Display.getHeight();
			}
			if (w <= 0 || h <= 0 || (w == client.width && h == client.height)) return;
			client.width = w;
			client.height = h;
			if (currentScreen != null) onResolutionChanged(w, h);
			resizeFramebuffer();
		} catch (Throwable ignored) {
		}
	}

	@Unique private static boolean optic$started;
	@Unique private boolean optic$rightShiftDown;

	/** Grab the finished Mojang loading screen just before it is shown, for the fade into the menu. */
	@Inject(
		method = "method_2915()V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;method_6648()V"),
		require = 0
	)
	private void optic$captureSplash(CallbackInfo ci) {
		gg.optic.client.gui.OpticMenuChrome.captureSplash();
	}

	/** The game always opens in a window (the launcher's --fullscreen and the saved option are ignored). */
	@Inject(method = "initializeGame()V", at = @At("HEAD"))
	private void optic$startWindowed(CallbackInfo ci) {
		fullscreen = false;
	}

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
		// Borderless is a fullscreen look; the game starts windowed, so it is not applied on launch.
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

	/**
	 * 1.7 leaves fullscreen whenever the window loses focus (alt-tab), so tabbing out dropped
	 * you to windowed mode. 1.8 removed this; do the same and stay fullscreen.
	 */
	@Redirect(
		method = "runGameLoop()V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;toggleFullscreen()V"),
		require = 0
	)
	private void optic$keepFullscreenOnTabOut(MinecraftClient client) {
	}

	/** Zoom scroll and Disable Hotbar Scroll read the wheel before the hotbar does. */
	@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
	private int optic$scrollWheel() {
		int wheel = Mouse.getEventDWheel();
		if (wheel != 0) optic$vanillaWheel = true;
		return optic$filterWheel(wheel);
	}

	@Unique private boolean optic$vanillaWheel;

	@Unique
	private int optic$filterWheel(int wheel) {
		if (OpticZoom.handleScroll(wheel)) return 0;
		if (OpticClient.get().config().disableMouseScrollWheel && currentScreen == null) return 0;
		return wheel;
	}

	/**
	 * With Raw Input on, LWJGL may never see the wheel. If this tick had no LWJGL wheel event
	 * but raw input reported one, scroll the hotbar from that instead.
	 */
	@Unique
	private void optic$rawWheelFallback(MinecraftClient client) {
		int raw = OpticRawInput.consumeWheel();
		boolean vanilla = optic$vanillaWheel;
		optic$vanillaWheel = false;
		if (raw == 0 || vanilla || currentScreen != null || client.field_3805 == null) return;
		int wheel = optic$filterWheel(raw);
		if (wheel != 0) client.field_3805.inventory.scrollInHotbar(wheel);
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
		optic$fitWindow(client);
		gg.optic.client.hud.OpticHudAnchor.tick(client);
		if (!(currentScreen instanceof OpticModScreen)) optic$rawWheelFallback(client);
		boolean itemInfo = OpticClient.get().config().itemInfo;
		if (options.heldItemTooltips != itemInfo) options.heldItemTooltips = itemInfo;
		if (OpticClient.get().config().toggleSprint && options.forwardKey.isPressed()) {
			if (client.field_3805 != null && optic$canSprint(client.field_3805)) {
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
	 * Same rules vanilla uses to start and keep a sprint. Without the collision check, Toggle
	 * Sprint switched sprint back on every tick after running into a wall, so you kept full
	 * sprint speed against the block and shot off when sliding past it.
	 */
	@Unique
	private static boolean optic$canSprint(net.minecraft.entity.player.ControllablePlayerEntity player) {
		if (player.isSneaking() || player.horizontalCollision || player.isUsingItem()) return false;
		if (player.input != null && player.input.movementForward < 0.8F) return false;
		if (player.hasStatusEffect(net.minecraft.entity.effect.StatusEffect.BLINDNESS)) return false;
		return player.getHungerManager().getFoodLevel() > 6 || player.abilities.allowFlying;
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
