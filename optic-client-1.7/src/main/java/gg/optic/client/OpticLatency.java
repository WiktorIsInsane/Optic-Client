package gg.optic.client;

import org.lwjgl.opengl.ARBSync;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLSync;

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

	/** Fence placed after the last frame; waited on after the next swap. */
	private static GLSync previousFrame;
	private static boolean fencesBroken;

	/**
	 * After the frame is swapped to the screen. Waits only until the PREVIOUS frame has
	 * finished on the GPU, so at most one frame is ever queued (like "max pre-rendered frames 1")
	 * while CPU and GPU still overlap. A full glFinish every frame stalled both and made frame
	 * times jump.
	 */
	public static void afterSwap() {
		OpticConfig c = config();
		if (c == null || !c.lowLatencyMode || fencesBroken) {
			dropFence();
			return;
		}
		try {
			ContextCapabilities caps = GLContext.getCapabilities();
			boolean core = caps.OpenGL32;
			if (!core && !caps.GL_ARB_sync) return;
			if (previousFrame != null) {
				// Normally returns at once (the GPU is a frame behind at most); capped at 20 ms.
				if (core) GL32.glClientWaitSync(previousFrame, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, 20000000L);
				else ARBSync.glClientWaitSync(previousFrame, ARBSync.GL_SYNC_FLUSH_COMMANDS_BIT, 20000000L);
				dropFence();
			}
			previousFrame = core
				? GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
				: ARBSync.glFenceSync(ARBSync.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
		} catch (Throwable t) {
			// Driver without working fences: stay off rather than risk anything.
			fencesBroken = true;
			previousFrame = null;
		}
	}

	/** Forget the fence (setting off, or the window/context was recreated). */
	public static void dropFence() {
		if (previousFrame == null) return;
		try {
			if (GLContext.getCapabilities().OpenGL32) GL32.glDeleteSync(previousFrame);
			else ARBSync.glDeleteSync(previousFrame);
		} catch (Throwable ignored) {
		}
		previousFrame = null;
	}
}
