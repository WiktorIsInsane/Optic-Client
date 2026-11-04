package gg.optic.client.input;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import gg.optic.client.OpticClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import org.lwjgl.opengl.Display;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * True 1:1 Windows raw mouse via WM_INPUT (HID mickeys, no pointer ballistics / accel).
 * Hook is installed on the client thread (required for SetWindowLongPtr).
 */
public final class OpticRawInput {
	private static final int WM_INPUT = 0x00FF;
	private static final int RID_INPUT = 0x10000003;
	private static final int RIM_TYPEMOUSE = 0;
	private static final int RIDEV_INPUTSINK = 0x00000100;
	private static final int RIDEV_REMOVE = 0x00000001;
	private static final int MOUSE_MOVE_ABSOLUTE = 0x01;
	private static final int GWL_WNDPROC = -4;

	private static volatile boolean wantEnabled;
	private static volatile boolean hooked;
	private static final AtomicInteger DX = new AtomicInteger();
	private static final AtomicInteger DY = new AtomicInteger();
	private static final int RI_MOUSE_WHEEL = 0x0400;
	/**
	 * Wheel delta seen through WM_INPUT. Registering the window for raw mouse input can stop
	 * LWJGL from getting WM_MOUSEWHEEL, so callers use this when no LWJGL wheel event came in.
	 */
	private static final AtomicInteger WHEEL = new AtomicInteger();

	private static WinDef.HWND hwnd;
	private static long hookedHwnd;
	private static BaseTSD.LONG_PTR oldWndProc;
	/** Kept as a field so the native callback is not GC'd. */
	private static WndProc wndProc;
	private static Memory rawBuffer;

	private OpticRawInput() {
	}

	public interface RawUser32 extends StdCallLibrary {
		@SuppressWarnings("deprecation")
		RawUser32 INSTANCE = (RawUser32) Native.loadLibrary("user32", RawUser32.class, W32APIOptions.DEFAULT_OPTIONS);

		boolean RegisterRawInputDevices(Pointer pRawInputDevices, int uiNumDevices, int cbSize);

		int GetRawInputData(WinNT.HANDLE hRawInput, int uiCommand, Pointer pData, IntByReference pcbSize, int cbSizeHeader);

		BaseTSD.LONG_PTR SetWindowLongPtr(WinDef.HWND hWnd, int nIndex, WndProc wndProc);

		BaseTSD.LONG_PTR SetWindowLongPtr(WinDef.HWND hWnd, int nIndex, BaseTSD.LONG_PTR dwNewLong);

		WinDef.LRESULT CallWindowProc(BaseTSD.LONG_PTR lpPrevWndFunc, WinDef.HWND hWnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam);
	}

	public interface WndProc extends StdCallLibrary.StdCallCallback {
		WinDef.LRESULT callback(WinDef.HWND hWnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam);
	}

	public static boolean isSupported() {
		return gg.optic.client.compat.Mc17.isWindows();
	}

	public static boolean isActive() {
		return wantEnabled && isSupported() && hooked;
	}

	public static boolean isRunning() {
		return wantEnabled;
	}

	public static synchronized void setEnabled(boolean enabled) {
		if (!isSupported()) {
			OpticClient.get().config().rawMouseInput = false;
			wantEnabled = false;
			return;
		}
		wantEnabled = enabled;
		if (!enabled) {
			uninstallHook();
			DX.set(0);
			DY.set(0);
		}
	}

	public static synchronized void start() {
		setEnabled(true);
	}

	public static synchronized void stop() {
		setEnabled(false);
	}

	public static synchronized void rescan() {
		DX.set(0);
		DY.set(0);
		uninstallHook();
	}

	/** Call after F11 / borderless / any Display recreate so WM_INPUT rebinds to the new HWND. */
	public static synchronized void onDisplayChanged() {
		if (!isSupported() || !wantEnabled) return;
		DX.set(0);
		DY.set(0);
		uninstallHook();
	}

	/** Called from the client tick on the main thread. */
	public static void tickClientThread() {
		if (!isSupported()) return;
		if (wantEnabled) {
			if (!Display.isCreated()) return;
			try {
				long currentHwnd = findHwnd();
				boolean hwndChanged = hooked && currentHwnd != 0L && currentHwnd != hookedHwnd;
				if (hwndChanged) {
					uninstallHook();
				}
				if (!hooked) {
					installHook();
				}
			} catch (Throwable t) {
				System.err.println("[Optic] Raw input hook failed: " + t.getMessage());
				t.printStackTrace();
				wantEnabled = false;
				OpticClient.get().config().rawMouseInput = false;
				uninstallHook();
			}
		} else if (hooked) {
			uninstallHook();
		}
	}

	/** Raw wheel delta since the last call (120 per notch), 0 when raw input is off. */
	public static int consumeWheel() {
		int wheel = WHEEL.getAndSet(0);
		return isActive() ? wheel : 0;
	}

	public static int consumeDx() {
		return DX.getAndSet(0);
	}

	public static int consumeDy() {
		// Windows +Y is down; Minecraft look +Y is up.
		return -DY.getAndSet(0);
	}

	private static void installHook() throws Exception {
		if (hooked) return;
		long handle = findHwnd();
		if (handle == 0L) throw new IllegalStateException("Could not find game HWND");
		hwnd = new WinDef.HWND(Pointer.createConstant(handle));
		hookedHwnd = handle;

		RawInputDevice rid = new RawInputDevice();
		rid.usUsagePage = 1;
		rid.usUsage = 2;
		rid.dwFlags = RIDEV_INPUTSINK;
		rid.hwndTarget = hwnd;
		rid.write();
		if (!RawUser32.INSTANCE.RegisterRawInputDevices(rid.getPointer(), 1, rid.size())) {
			throw new IllegalStateException("RegisterRawInputDevices failed (" + Native.getLastError() + ")");
		}

		rawBuffer = new Memory(256);

		wndProc = new WndProc() {
			@Override
			public WinDef.LRESULT callback(WinDef.HWND hWnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
				if (uMsg == WM_INPUT && wantEnabled) {
					onRawInput(lParam);
				}
				return RawUser32.INSTANCE.CallWindowProc(oldWndProc, hWnd, uMsg, wParam, lParam);
			}
		};

		oldWndProc = RawUser32.INSTANCE.SetWindowLongPtr(hwnd, GWL_WNDPROC, wndProc);
		hooked = true;
		DX.set(0);
		DY.set(0);
		System.out.println("[Optic] Raw input active — WM_INPUT 1:1 (no Windows accel)");
	}

	private static void uninstallHook() {
		if (!hooked) {
			hwnd = null;
			hookedHwnd = 0L;
			oldWndProc = null;
			wndProc = null;
			rawBuffer = null;
			return;
		}
		if (hwnd != null && oldWndProc != null) {
			try {
				RawUser32.INSTANCE.SetWindowLongPtr(hwnd, GWL_WNDPROC, oldWndProc);
			} catch (Throwable ignored) {
			}
		}
		if (hwnd != null) {
			try {
				RawInputDevice rid = new RawInputDevice();
				rid.usUsagePage = 1;
				rid.usUsage = 2;
				rid.dwFlags = RIDEV_REMOVE;
				rid.hwndTarget = null;
				rid.write();
				RawUser32.INSTANCE.RegisterRawInputDevices(rid.getPointer(), 1, rid.size());
			} catch (Throwable ignored) {
			}
		}
		hwnd = null;
		hookedHwnd = 0L;
		oldWndProc = null;
		wndProc = null;
		rawBuffer = null;
		hooked = false;
	}

	private static void onRawInput(WinDef.LPARAM lParam) {
		try {
			Memory buf = rawBuffer;
			if (buf == null) return;

			int headerSize = 8 + Native.POINTER_SIZE * 2;
			IntByReference pcbSize = new IntByReference((int) buf.size());
			WinNT.HANDLE hRaw = new WinNT.HANDLE(Pointer.createConstant(lParam.longValue()));

			int read = RawUser32.INSTANCE.GetRawInputData(hRaw, RID_INPUT, buf, pcbSize, headerSize);
			if (read == -1 || read == 0) return;

			int type = buf.getInt(0);
			if (type != RIM_TYPEMOUSE) return;

			// RAWMOUSE after header (MSVC): usFlags@0, union@4, ulRawButtons@8, lLastX@12, lLastY@16
			// union = usButtonFlags@4, usButtonData@6
			int buttonFlags = buf.getShort(headerSize + 4) & 0xFFFF;
			if ((buttonFlags & RI_MOUSE_WHEEL) != 0) {
				WHEEL.addAndGet(buf.getShort(headerSize + 6));
			}
			int flags = buf.getShort(headerSize) & 0xFFFF;
			if ((flags & MOUSE_MOVE_ABSOLUTE) != 0) return;

			int dx = buf.getInt(headerSize + 12);
			int dy = buf.getInt(headerSize + 16);
			if (dx == 0 && dy == 0) return;

			MinecraftClient client = MinecraftClient.getInstance();
			if (client != null && client.currentScreen == null) {
				DX.addAndGet(dx);
				DY.addAndGet(dy);
			}
		} catch (Throwable ignored) {
		}
	}

	private static long findHwnd() throws Exception {
		try {
			Method getHandle = Display.class.getMethod("getHandle");
			Object result = getHandle.invoke(null);
			if (result instanceof Long) return ((Long) result).longValue();
			if (result instanceof Integer) return ((Integer) result).longValue();
		} catch (Throwable ignored) {
		}

		Method getImpl = Display.class.getDeclaredMethod("getImplementation");
		getImpl.setAccessible(true);
		Object impl = getImpl.invoke(null);
		if (impl == null) return 0L;

		Class<?> cls = impl.getClass();
		while (cls != null) {
			String[] names = { "hwnd", "window", "handle" };
			for (int i = 0; i < names.length; i++) {
				try {
					Field f = cls.getDeclaredField(names[i]);
					f.setAccessible(true);
					Object v = f.get(impl);
					if (v instanceof Long) return ((Long) v).longValue();
					if (v instanceof Integer) return ((Integer) v).longValue();
					if (v instanceof Pointer) return Pointer.nativeValue((Pointer) v);
				} catch (NoSuchFieldException ignored) {
				}
			}
			cls = cls.getSuperclass();
		}
		return 0L;
	}

	public static class RawInputDevice extends Structure {
		public short usUsagePage;
		public short usUsage;
		public int dwFlags;
		public WinDef.HWND hwndTarget;

		public RawInputDevice() {
			super();
			setAlignType(ALIGN_DEFAULT);
			allocateMemory();
		}

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("usUsagePage", "usUsage", "dwFlags", "hwndTarget");
		}
	}
}
