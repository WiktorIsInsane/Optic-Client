package gg.optic.client;

import gg.optic.client.gui.OpticHudEditorScreen;
import gg.optic.client.gui.OpticModScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/** Helpers for Optic click-GUI / edit-GUI awareness. */
public final class OpticGui {
	private OpticGui() {
	}

	public static boolean isOpticScreen(Screen screen) {
		return screen instanceof OpticModScreen || screen instanceof OpticHudEditorScreen;
	}

	public static boolean isOpticScreenOpen() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client != null && isOpticScreen(client.currentScreen);
	}

	/** Click GUI (mod menu) — hide live HUD overlays. */
	public static boolean isModMenuOpen() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client != null && client.currentScreen instanceof OpticModScreen;
	}
}
