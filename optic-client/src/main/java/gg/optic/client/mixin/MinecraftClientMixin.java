package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticGui;
import gg.optic.client.OpticNet;
import gg.optic.client.OpticZoom;
import gg.optic.client.account.MicrosoftAuth;
import gg.optic.client.account.SessionSwap;
import gg.optic.client.account.SharedAccount;
import gg.optic.client.account.SharedAccounts;
import gg.optic.client.display.OpticBorderless;
import gg.optic.client.gui.OpticHudEditorScreen;
import gg.optic.client.gui.OpticModScreen;
import gg.optic.client.hud.OpticGuiScale;
import gg.optic.client.hud.OpticHud;
import gg.optic.client.hud.OpticTab;
import gg.optic.client.hud.OpticTitles;
import gg.optic.client.render.OpticHitPredict;
import gg.optic.client.input.OpticRawInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Session;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reserved for Optic's game-loop, input and performance hooks.
 * Keeping this mixin loaded from day one gives the standalone client a stable
 * integration point without depending on proprietary client classes.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements SessionSwap.SessionHolder {
	@Shadow public net.minecraft.client.gui.screen.Screen currentScreen;
	@Shadow public net.minecraft.client.option.GameOptions options;

	@Shadow
	@Final
	@Mutable
	private Session session;

	@Shadow
	private int attackCooldown;

	private boolean optic$rightShiftDown;
	private boolean optic$prioritySet;

	@Shadow public abstract void setScreen(net.minecraft.client.gui.screen.Screen screen);

	@Override
	public void optic$setSession(Session session) {
		this.session = session;
	}

	private static void optic$refreshMicrosoft(MinecraftClient client) {
		try {
			SharedAccount active = SharedAccounts.load().active();
			if (active == null || !"microsoft".equalsIgnoreCase(active.kind)) return;
			SharedAccount fresh = MicrosoftAuth.refresh(active);
			if (fresh == null) return;
			SessionSwap.applyNew(client, fresh);
			System.out.println("[Optic] Microsoft session refreshed for " + fresh.username);
		} catch (Exception e) {
			System.err.println("[Optic] Microsoft session refresh failed: " + e.getMessage());
		}
	}

	@Inject(method = "initializeGame()V", at = @At("TAIL"))
	private void optic$initialized(CallbackInfo ci) {
		optic$refreshMicrosoft((MinecraftClient) (Object) this);
		Display.setTitle("Optic 1.8.9");
		options.heldItemTooltips = OpticClient.get().config().itemInfo;
		if (OpticClient.get().config().fullbright) {
			options.gamma = OpticClient.get().config().fullbrightGamma;
		}
		if (OpticClient.get().config().rawMouseInput) {
			OpticRawInput.setEnabled(true);
		}
		if (OpticClient.get().config().borderlessFullscreen) {
			OpticBorderless.apply(true);
		}
	}

	@Inject(method = "stop()V", at = @At("HEAD"))
	private void optic$saveOnExit(CallbackInfo ci) {
		OpticClient.get().config().save();
	}

	@Inject(method = "printCrashReport(Lnet/minecraft/util/crash/CrashReport;)V", at = @At("HEAD"))
	private void optic$saveOnCrash(CallbackInfo ci) {
		try {
			OpticClient.get().config().save();
		} catch (Throwable ignored) {
		}
	}

	/** glGetError waits on the GPU twice a frame and prints the GL ERROR spam. */
	@Inject(method = "setGlErrorMessage(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
	private void optic$skipGlErrorSync(CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "updateDisplay()V", at = @At("HEAD"))
	private void optic$paceBeforeSwap(CallbackInfo ci) {
		gg.optic.client.render.OpticFramePacing.beforeSwap();
	}

	@Inject(method = "updateDisplay()V", at = @At("RETURN"))
	private void optic$paceAfterSwap(CallbackInfo ci) {
		gg.optic.client.render.OpticFramePacing.afterSwap();
	}

	/** Menus are hard-capped at 30, which sleeps the thread. Use the same cap as in-game. */
	@Inject(method = "getMaxFramerate()I", at = @At("HEAD"), cancellable = true)
	private void optic$menuFps(CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(options.maxFramerate);
	}

	/**
	 * The render thread yields every frame. On Windows that is a full scheduler
	 * gap, so frame times bunch up and input waits behind the next timeslice.
	 */
	@Redirect(method = "runGameLoop()V", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V"))
	private void optic$noYield() {
	}

	@Inject(method = "runGameLoop()V", at = @At("HEAD"))
	private void optic$renderPriority(CallbackInfo ci) {
		if (!optic$prioritySet) {
			optic$prioritySet = true;
			Thread me = Thread.currentThread();
			int target = Thread.NORM_PRIORITY + 3;
			if (me.getPriority() < target) me.setPriority(target);
		}
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
		if (options.heldItemTooltips != itemInfo) {
			options.heldItemTooltips = itemInfo;
		}
		if (OpticClient.get().config().toggleSprint && options.forwardKey.isPressed()) {
			if (client.player != null && !client.player.isSneaking()) {
				client.player.setSprinting(true);
			}
		}
		OpticZoom.tick();
		OpticHud.tickCopy(client);
		OpticTab.tick(client);
		OpticTitles.tick(client);
		gg.optic.client.render.OpticNametags.tick(client);
		// The tick's look and position are written by now. Push them out immediately.
		OpticNet.flush(client);
	}

	@Inject(method = "setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
	private void optic$inventoryScale(net.minecraft.client.gui.screen.Screen screen, CallbackInfo ci) {
		OpticGuiScale.beforeScreen((MinecraftClient) (Object) this, screen);
	}

	/**
	 * Built-in no click delay. 1.8 sets attackCooldown to 10 ticks after a miss,
	 * which drops the next swing for half a second. The server still applies its
	 * own 10-tick damage immunity. This only clears that client wait.
	 */
	@Inject(method = "doAttack()V", at = @At("HEAD"), cancellable = true)
	private void optic$leftClick(CallbackInfo ci) {
		if (this.currentScreen != null) return;
		// A miss must not arm the 10-tick client wait. Server immunity is untouched.
		this.attackCooldown = 0;
		// The target stays the one picked for the last drawn frame, as in vanilla and Lunar.
		// Re-picking here at tickDelta 1.0 aimed up to a tick ahead of the screen and
		// dropped hits that were under the crosshair.
		if (!OpticGui.isOpticScreenOpen()) {
			OpticClient.get().clicks().click(0);
		}
	}

	@Inject(method = "doAttack()V", at = @At("RETURN"))
	private void optic$noHitDelay(CallbackInfo ci) {
		if (this.currentScreen == null) this.attackCooldown = 0;
		MinecraftClient client = (MinecraftClient) (Object) this;
		if (OpticClient.get().config().cpsIgnoreCancelled && client.player != null && !client.player.handSwinging) {
			OpticClient.get().clicks().undo(0);
		}
		OpticHitPredict.onSwing(client);
		OpticNet.flush(client);
	}

	@ModifyConstant(method = "doAttack()V", constant = @Constant(intValue = 10))
	private int optic$noMissCooldown(int original) {
		return 0;
	}

	@Inject(method = "doUse()V", at = @At("HEAD"))
	private void optic$rightClick(CallbackInfo ci) {
		if (!OpticGui.isOpticScreenOpen()) {
			OpticClient.get().clicks().click(1);
		}
	}

	@Redirect(
		method = "tick()V",
		at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false)
	)
	private int optic$disableScrollWheel() {
		int wheel = Mouse.getEventDWheel();
		if (OpticZoom.handleScroll(wheel)) return 0;
		// Only block in-world hotbar slot changes — GUIs/chat keep scrolling.
		if (OpticClient.get().config().disableMouseScrollWheel && this.currentScreen == null) {
			return 0;
		}
		return wheel;
	}

	@Inject(method = "toggleFullscreen()V", at = @At("RETURN"))
	private void optic$rawMouseAfterFullscreen(CallbackInfo ci) {
		OpticRawInput.onDisplayChanged();
	}
}
