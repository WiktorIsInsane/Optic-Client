package gg.optic.client.hud;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

/** Inventory, chest, and menu GUI scale. Crosshair size is read by the crosshair renderer. */
public final class OpticGuiScale {
	private static int saved = -1;

	private OpticGuiScale() {
	}

	public static void beforeScreen(MinecraftClient client, Screen screen) {
		if (client == null || client.options == null) return;
		OpticConfig c = OpticClient.get().config();
		boolean container = screen instanceof HandledScreen || screen instanceof InventoryScreen;
		int forced = 0;
		if (c.guiScale && screen != null) {
			if (container && c.inventoryScale > 0) forced = c.inventoryScale;
			else if (!container && c.menuScale > 0) forced = c.menuScale;
		}
		if (forced > 0) {
			if (saved < 0) saved = client.options.guiScale;
			client.options.guiScale = forced;
		} else if (saved >= 0) {
			client.options.guiScale = saved;
			saved = -1;
		}
	}

	public static float hotbarScale(OpticConfig c) {
		return 1.0F;
	}
}
