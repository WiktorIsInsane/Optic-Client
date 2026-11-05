package gg.optic.client.display;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import gg.optic.client.input.OpticRawInput;
import net.minecraft.client.MinecraftClient;

/**
 * Fullscreen modes (F11):
 * - Borderless on: the game window loses its frame and covers the whole monitor. No display
 *   mode change, so alt-tab is instant.
 * - Borderless off: vanilla's real (exclusive) fullscreen.
 * LWJGL 2 only reads its "undecorated" flag when the window is created, so the frame is taken
 * off (and put back) with the Windows window style calls instead.
 */
public final class OpticBorderless {
	private static final int GWL_STYLE = -16;
	private static final int WS_CAPTION = 0x00C00000;
	private static final int WS_THICKFRAME = 0x00040000;
	private static final int WS_MINIMIZEBOX = 0x00020000;
	private static final int WS_MAXIMIZEBOX = 0x00010000;
	private static final int WS_SYSMENU = 0x00080000;
	private static final int WS_POPUP = 0x80000000;
	private static final int SWP_NOZORDER = 0x0004;
	private static final int SWP_NOOWNERZORDER = 0x0200;
	private static final int SWP_FRAMECHANGED = 0x0020;
	private static final int SWP_SHOWWINDOW = 0x0040;

	private static boolean applied;
	private static int savedStyle;
	private static WinDef.RECT savedRect;
	private static int width;
	private static int height;

	private OpticBorderless() {
	}

	public static boolean isApplied() {
		return applied;
	}

	/** Size of the borderless window (the monitor), for the game to render at. */
	public static int width() {
		return width;
	}

	public static int height() {
		return height;
	}

	private static boolean supported() {
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}

	/** F11. @return true when handled here; false means let vanilla toggle real fullscreen. */
	public static boolean onToggleFullscreen(boolean setting, boolean exclusiveFullscreen) {
		if (applied) {
			exit();
			return true;
		}
		if (setting && !exclusiveFullscreen && supported()) return enter();
		return false;
	}

	/**
	 * The Borderless setting was flipped. Only matters while fullscreen: swap to the other kind
	 * right away so what you see matches the setting.
	 */
	public static void apply(boolean setting) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) return;
		if (setting && client.options != null && isExclusive(client)) {
			client.toggleFullscreen();
			enter();
		} else if (!setting && applied) {
			exit();
			client.toggleFullscreen();
		}
	}

	private static boolean isExclusive(MinecraftClient client) {
		try {
			return org.lwjgl.opengl.Display.isFullscreen();
		} catch (Throwable t) {
			return false;
		}
	}

	private static boolean enter() {
		try {
			WinDef.HWND hwnd = hwnd();
			if (hwnd == null) return false;
			User32 u = User32.INSTANCE;
			savedStyle = u.GetWindowLong(hwnd, GWL_STYLE);
			savedRect = new WinDef.RECT();
			u.GetWindowRect(hwnd, savedRect);

			WinUser.HMONITOR monitor = u.MonitorFromWindow(hwnd, WinUser.MONITOR_DEFAULTTONEAREST);
			WinUser.MONITORINFO info = new WinUser.MONITORINFO();
			u.GetMonitorInfo(monitor, info);
			WinDef.RECT m = info.rcMonitor;

			int style = (savedStyle & ~(WS_CAPTION | WS_THICKFRAME | WS_MINIMIZEBOX | WS_MAXIMIZEBOX | WS_SYSMENU)) | WS_POPUP;
			u.SetWindowLong(hwnd, GWL_STYLE, style);
			width = m.right - m.left;
			height = m.bottom - m.top;
			u.SetWindowPos(hwnd, null, m.left, m.top, width, height, SWP_NOZORDER | SWP_NOOWNERZORDER | SWP_FRAMECHANGED | SWP_SHOWWINDOW);
			applied = true;
			OpticRawInput.onDisplayChanged();
			return true;
		} catch (Throwable t) {
			System.err.println("[Optic] Borderless fullscreen failed: " + t);
			return false;
		}
	}

	private static void exit() {
		applied = false;
		try {
			WinDef.HWND hwnd = hwnd();
			if (hwnd == null) return;
			User32 u = User32.INSTANCE;
			u.SetWindowLong(hwnd, GWL_STYLE, savedStyle);
			WinDef.RECT r = savedRect;
			if (r != null) {
				u.SetWindowPos(hwnd, null, r.left, r.top, r.right - r.left, r.bottom - r.top, SWP_NOZORDER | SWP_NOOWNERZORDER | SWP_FRAMECHANGED | SWP_SHOWWINDOW);
			}
			OpticRawInput.onDisplayChanged();
		} catch (Throwable t) {
			System.err.println("[Optic] Leaving borderless fullscreen failed: " + t);
		}
	}

	private static WinDef.HWND hwnd() throws Exception {
		long handle = OpticRawInput.findHwnd();
		return handle == 0L ? null : new WinDef.HWND(Pointer.createConstant(handle));
	}
}
