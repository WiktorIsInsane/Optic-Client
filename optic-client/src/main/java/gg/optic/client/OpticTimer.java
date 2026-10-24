package gg.optic.client;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * Windows schedules sleeps on a 15.6ms quantum unless a process asks for 1ms.
 * Minecraft's tick, sound, and frame limiter all sleep, so that quantum shows
 * up as bunched frames. High priority keeps the game ahead of background work.
 * Realtime is avoided because it starves the compositor and audio.
 * Affinity is left on every core: a 9800X3D is one chiplet with the cache
 * shared by all cores, so pinning a thread to one core only adds stalls.
 */
public final class OpticTimer {
	private static final int HIGH_PRIORITY_CLASS = 0x00000080;

	private OpticTimer() {
	}

	public interface Winmm extends StdCallLibrary {
		@SuppressWarnings("deprecation")
		Winmm INSTANCE = (Winmm) Native.loadLibrary("winmm", Winmm.class, W32APIOptions.DEFAULT_OPTIONS);

		int timeBeginPeriod(int period);

		int timeEndPeriod(int period);
	}

	public interface Kernel extends StdCallLibrary {
		@SuppressWarnings("deprecation")
		Kernel INSTANCE = (Kernel) Native.loadLibrary("kernel32", Kernel.class, W32APIOptions.DEFAULT_OPTIONS);

		Pointer GetCurrentProcess();

		boolean SetPriorityClass(Pointer process, int priorityClass);
	}

	public static void begin() {
		String os = System.getProperty("os.name", "");
		if (os.toLowerCase().indexOf("win") < 0) return;
		try {
			if (Winmm.INSTANCE.timeBeginPeriod(1) == 0) {
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Winmm.INSTANCE.timeEndPeriod(1);
						} catch (Throwable ignored) {
						}
					}
				}, "optic-timer"));
			}
		} catch (Throwable t) {
			System.err.println("[Optic] 1ms timer unavailable: " + t.getMessage());
		}
		try {
			Kernel.INSTANCE.SetPriorityClass(Kernel.INSTANCE.GetCurrentProcess(), HIGH_PRIORITY_CLASS);
		} catch (Throwable t) {
			System.err.println("[Optic] Process priority unchanged: " + t.getMessage());
		}
		Thread me = Thread.currentThread();
		int target = Thread.NORM_PRIORITY + 3;
		if (me.getPriority() < target) me.setPriority(target);
	}
}
