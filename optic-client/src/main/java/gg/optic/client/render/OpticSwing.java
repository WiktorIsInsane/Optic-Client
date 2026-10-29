package gg.optic.client.render;

import gg.optic.client.OpticClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Starts the arm swing on the frame the attack button goes down. Vanilla waits for
 * the next tick to read the click and one more tick before the swing moves, so the
 * hand trails the click by 50-100 ms. This only changes how the swing is drawn:
 * the swing and attack packets still go out on the tick, exactly as vanilla.
 */
public final class OpticSwing {
	private static final double SWING_NANOS = 6 * 50_000_000.0;

	private static boolean wasDown;
	private static long startedAt;
	private static boolean settling;

	private OpticSwing() {
	}

	/** Called once per frame before the world is drawn. */
	public static void frame(MinecraftClient client) {
		boolean down = client != null && client.currentScreen == null && client.player != null && pressed(client.options.attackKey);
		if (down && !wasDown && enabled() && !client.player.isUsingItem() && aimingAtEntityOrAir(client)) {
			long now = System.nanoTime();
			// Same rule as vanilla: a new click restarts the swing once it is half done.
			if (startedAt == 0L || now - startedAt >= SWING_NANOS / 2) {
				startedAt = now;
				settling = false;
			}
		}
		wasDown = down;
	}

	/** Swing progress for the local player while a frame-started swing is playing, else -1. */
	public static float progress(LivingEntity player) {
		if (!enabled()) {
			startedAt = 0L;
			settling = false;
			return -1.0F;
		}
		if (startedAt != 0L) {
			double t = (System.nanoTime() - startedAt) / SWING_NANOS;
			if (t < 1.0) return (float) t;
			startedAt = 0L;
			settling = true;
		}
		// The tick swing started up to a tick later and is still finishing. Keep the
		// arm at rest instead of letting it jump back into the middle of a swing.
		if (settling) {
			if (player.handSwinging) return 0.0F;
			settling = false;
		}
		return -1.0F;
	}

	private static boolean enabled() {
		return OpticClient.get().config().instantSwing;
	}

	/** Mining keeps vanilla's tick swing so holding on a block does not double up. */
	private static boolean aimingAtEntityOrAir(MinecraftClient client) {
		BlockHitResult hit = client.result;
		return hit == null || hit.type != BlockHitResult.Type.BLOCK;
	}

	private static boolean pressed(KeyBinding binding) {
		int code = binding.getCode();
		return code < 0 ? Mouse.isButtonDown(code + 100) : Keyboard.isKeyDown(code);
	}
}
