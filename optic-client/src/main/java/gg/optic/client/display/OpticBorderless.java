package gg.optic.client.display;

import gg.optic.client.input.OpticRawInput;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/**
 * Borderless fullscreen for LWJGL 2: undecorated windowed mode at desktop resolution.
 * Avoids exclusive fullscreen mode switches (alt-tab friendly).
 */
public final class OpticBorderless {
	private static DisplayMode previousMode;
	private static boolean applied;

	private OpticBorderless() {
	}

	public static void apply(boolean enabled) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) return;
		try {
			if (enabled) {
				if (!applied) {
					previousMode = Display.getDisplayMode();
				}
				System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
				if (Display.isFullscreen()) {
					Display.setFullscreen(false);
				}
				DisplayMode desktop = Display.getDesktopDisplayMode();
				Display.setDisplayMode(desktop);
				Display.setResizable(false);
				Display.setLocation(0, 0);
				client.width = desktop.getWidth();
				client.height = desktop.getHeight();
				applied = true;
			} else if (applied) {
				System.setProperty("org.lwjgl.opengl.Window.undecorated", "false");
				Display.setResizable(true);
				DisplayMode restore = previousMode != null
					? previousMode
					: new DisplayMode(Math.max(854, client.width), Math.max(480, client.height));
				if (client.options != null && client.options.fullscreen) {
					Display.setDisplayMode(Display.getDesktopDisplayMode());
					Display.setFullscreen(true);
				} else {
					Display.setFullscreen(false);
					Display.setDisplayMode(restore);
				}
				applied = false;
			}
			OpticRawInput.onDisplayChanged();
		} catch (Throwable t) {
			System.err.println("[Optic] Borderless fullscreen failed: " + t.getMessage());
		}
	}

	public static boolean isApplied() {
		return applied;
	}
}
