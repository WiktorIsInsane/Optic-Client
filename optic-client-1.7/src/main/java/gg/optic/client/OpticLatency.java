package gg.optic.client;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 * Frame pacing and input latency.
 *
 * Precise Frame Limiter: replaces LWJGL's Display.sync, which only sleeps in whole
 * milliseconds and drifts, so frame times wobble around the cap. This one sleeps until about
 * 1 ms before the frame is due and then waits out the rest exactly, keeping every frame the
 * same length.
 *
 * Low Latency Mode: after the buffer swap, waits for the GPU to finish (glFinish). Drivers
 * otherwise queue up to 3 frames ahead, so what you see lags your mouse by those frames;
 * this keeps the queue at one. Costs a little FPS when the GPU is the bottleneck.
 */
public final class OpticLatency {
	private static long nextFrame;

	private OpticLatency() {
	}

	private static OpticConfig config() {
		OpticClient optic = OpticClient.get();
		return optic == null ? null : optic.config();
	}

	/** Frame cap, replacing Display.sync(fps). */
	public static void sync(int fps) {
		OpticConfig c = config();
		if (c == null || !c.preciseFrameLimiter) {
			Display.sync(fps);
			return;
		}
		if (fps <= 0) return;
		long frame = 1000000000L / fps;
		long now = System.nanoTime();
		// First frame, or we fell more than a frame behind (a hitch): restart the schedule
		// instead of rushing several frames to catch up.
		if (nextFrame == 0L || now - nextFrame > frame) nextFrame = now;
		nextFrame += frame;
		while (true) {
			long left = nextFrame - System.nanoTime();
			if (left <= 0L) break;
			if (left > 1500000L) {
				try {
					Thread.sleep(1L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			} else if (left > 150000L) {
				Thread.yield();
			}
			// Under 0.15 ms: spin; the most exact wait there is.
		}
	}

	/** After the frame is swapped to the screen. */
	public static void afterSwap() {
		OpticConfig c = config();
		if (c != null && c.lowLatencyMode) GL11.glFinish();
	}
}
