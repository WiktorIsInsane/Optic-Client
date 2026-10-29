package gg.optic.client.render;

import gg.optic.client.OpticClient;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLSync;

/**
 * Low Latency Frames. With vsync off the driver lets the CPU run two or three frames
 * ahead of the GPU, so the frame on screen was built from older mouse input. A fence
 * after each present and a wait on it before the next one keeps one frame in flight.
 * Costs a little FPS; nothing about ticks or packets changes.
 */
public final class OpticFramePacing {
	/** Never stall a frame longer than this if the driver misbehaves. */
	private static final long MAX_WAIT_NANOS = 20_000_000L;

	private static GLSync pending;
	private static Boolean supported;

	private OpticFramePacing() {
	}

	/** Before the buffer swap: wait until the previous frame has left the GPU. */
	public static void beforeSwap() {
		if (pending == null) return;
		GLSync sync = pending;
		pending = null;
		try {
			GL32.glClientWaitSync(sync, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, MAX_WAIT_NANOS);
			GL32.glDeleteSync(sync);
		} catch (Throwable t) {
			supported = Boolean.FALSE;
		}
	}

	/** After the buffer swap: mark the point the next frame waits for. */
	public static void afterSwap() {
		if (!OpticClient.get().config().lowLatencyFrames || !supported()) return;
		try {
			pending = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
		} catch (Throwable t) {
			supported = Boolean.FALSE;
			pending = null;
		}
	}

	private static boolean supported() {
		if (supported == null) {
			try {
				supported = Boolean.valueOf(GLContext.getCapabilities().OpenGL32);
			} catch (Throwable t) {
				supported = Boolean.FALSE;
			}
		}
		return supported.booleanValue();
	}
}
