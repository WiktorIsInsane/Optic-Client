package gg.optic.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import org.lwjgl.input.Keyboard;

/**
 * Badlion-style hold-to-zoom (default C).
 * Scroll zoom persists between holds; FOV eases per-frame (not per-tick) so
 * scroll zoom is smooth instead of choppy 20-TPS keyframes.
 */
public final class OpticZoom {
	private static float progress;
	private static float progressTarget;
	/** Applied scroll zoom (frame-smoothed toward {@link #scrollTarget}). */
	private static float scrollFactor = 1.0F;
	/** Desired scroll zoom from the wheel (persists when you release C). */
	private static float scrollTarget = 1.0F;
	private static long lastSmoothNs;
	private static boolean forcedSmoothCamera;
	private static boolean previousSmoothCamera;
	private static float mouseCarryX;
	private static float mouseCarryY;

	private OpticZoom() {
	}

	public static void tick() {
		OpticConfig c = OpticClient.get().config();
		MinecraftClient client = MinecraftClient.getInstance();
		if (!c.zoom || client == null) {
			progress = 0.0F;
			progressTarget = 0.0F;
			scrollFactor = 1.0F;
			scrollTarget = 1.0F;
			lastSmoothNs = 0L;
			clearMouseCarry();
			restoreSmoothCamera(client);
			return;
		}

		boolean held = client.currentScreen == null && Keyboard.isKeyDown(c.zoomKeyCode);
		progressTarget = held ? 1.0F : 0.0F;
		if (!c.zoomSmooth) {
			progress = progressTarget;
		}

		if (!held) clearMouseCarry();

		if (held && c.zoomSmoothCamera) {
			GameOptions options = client.options;
			if (options != null) {
				if (!forcedSmoothCamera) {
					previousSmoothCamera = options.smoothCameraEnabled;
					forcedSmoothCamera = true;
				}
				options.smoothCameraEnabled = true;
			}
		} else {
			restoreSmoothCamera(client);
		}
	}

	/** Per-frame exponential smooth — call from FOV / sens paths. */
	private static void smoothVisuals() {
		OpticConfig c = OpticClient.get().config();
		long now = System.nanoTime();
		float dt;
		if (lastSmoothNs == 0L) {
			dt = 1.0F / 60.0F;
		} else {
			dt = (now - lastSmoothNs) * 1.0E-9F;
		}
		lastSmoothNs = now;
		if (dt < 0.0F) dt = 1.0F / 60.0F;
		if (dt > 0.05F) dt = 0.05F;

		// Hold zoom: fast ease (~instant feel, no tick stepping).
		if (c.zoomSmooth) {
			float holdRate = progressTarget > progress ? 14.0F : 16.0F;
			float holdA = 1.0F - (float) Math.exp(-holdRate * dt);
			progress += (progressTarget - progress) * holdA;
			if (Math.abs(progress - progressTarget) < 0.001F) progress = progressTarget;
		}

		// Scroll zoom: responsive but continuous (not laggy, not keyframed).
		float scrollRate = 18.0F;
		float scrollA = 1.0F - (float) Math.exp(-scrollRate * dt);
		scrollFactor += (scrollTarget - scrollFactor) * scrollA;
		if (Math.abs(scrollFactor - scrollTarget) < 0.0008F) scrollFactor = scrollTarget;
	}

	private static void restoreSmoothCamera(MinecraftClient client) {
		if (!forcedSmoothCamera) return;
		if (client != null && client.options != null) {
			client.options.smoothCameraEnabled = previousSmoothCamera;
		}
		forcedSmoothCamera = false;
	}

	private static void clearMouseCarry() {
		mouseCarryX = 0.0F;
		mouseCarryY = 0.0F;
	}

	/** Consume scroll while zooming; returns true if hotbar scroll should be blocked. */
	public static boolean handleScroll(int wheel) {
		OpticConfig c = OpticClient.get().config();
		if (!c.zoom || !c.zoomScroll || progress <= 0.01F || wheel == 0) return false;
		// Fine steps; frame smoothing does the rest.
		float speed = Math.max(0.05F, Math.min(1.0F, c.zoomScrollSpeed)) * 0.22F;
		float delta = wheel > 0 ? speed : -speed;
		scrollTarget = clamp(scrollTarget + delta, 0.35F, 3.0F);
		return true;
	}

	public static float fovMultiplier() {
		smoothVisuals();
		if (progress <= 0.001F) return 1.0F;
		OpticConfig c = OpticClient.get().config();
		float factor = Math.max(1.25F, c.zoomFactor) * scrollFactor;
		float zoomed = 1.0F / factor;
		return 1.0F + (zoomed - 1.0F) * progress;
	}

	public static float sensitivityScale() {
		smoothVisuals();
		if (progress <= 0.001F) return 1.0F;
		OpticConfig c = OpticClient.get().config();
		if (!c.zoomMouseSensitivity) return 1.0F;
		float factor = Math.max(1.25F, c.zoomFactor) * scrollFactor;
		float scaled = 1.0F / factor;
		return 1.0F + (scaled - 1.0F) * progress;
	}

	public static int scaleMouseDelta(int delta, boolean xAxis) {
		float scale = sensitivityScale();
		if (scale >= 0.999F) {
			clearMouseCarry();
			return delta;
		}
		if (xAxis) {
			mouseCarryX += delta * scale;
			int out = (int) mouseCarryX;
			mouseCarryX -= out;
			return out;
		}
		mouseCarryY += delta * scale;
		int out = (int) mouseCarryY;
		mouseCarryY -= out;
		return out;
	}

	public static boolean isActive() {
		return progress > 0.001F;
	}

	private static float clamp(float v, float min, float max) {
		return v < min ? min : (v > max ? max : v);
	}
}
