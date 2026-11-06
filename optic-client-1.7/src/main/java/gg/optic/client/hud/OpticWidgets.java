package gg.optic.client.hud;

import gg.optic.client.OpticConfig;
import gg.optic.client.gui.OpticModScreen;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD widgets that live outside the text HUD: the PotPvP pearl cooldown and the six graphs.
 * One place for what the HUD editor needs (enabled, position, scale, size, owning mod) and for
 * drawing them, so each can be dragged and resized on its own.
 */
public final class OpticWidgets {
	public static final String PEARL = "PEARL";
	public static final String GRAPH = "GRAPH";

	private OpticWidgets() {
	}

	public static boolean isWidget(String id) {
		return PEARL.equals(id) || (id != null && id.startsWith(GRAPH));
	}

	/** Widgets switched on right now, in draw order. */
	public static List<String> enabled(OpticConfig c) {
		List<String> ids = new ArrayList<String>();
		if (c.pvpTweaks && c.pvpPearlCooldown) ids.add(PEARL);
		if (c.graphs) {
			for (int i = 0; i < OpticGraphs.COUNT; i++) {
				if (c.graphOn[i]) ids.add(GRAPH + i);
			}
		}
		return ids;
	}

	public static int moduleOf(String id) {
		return PEARL.equals(id) ? OpticModScreen.MODULE_PVP_TWEAKS : OpticModScreen.MODULE_GRAPHS;
	}

	private static int graph(String id) {
		try {
			return Math.max(0, Math.min(OpticGraphs.COUNT - 1, Integer.parseInt(id.substring(GRAPH.length()))));
		} catch (Exception e) {
			return 0;
		}
	}

	public static int x(String id, OpticConfig c) {
		return PEARL.equals(id) ? c.pearlX : c.graphX[graph(id)];
	}

	public static int y(String id, OpticConfig c) {
		return PEARL.equals(id) ? c.pearlY : c.graphY[graph(id)];
	}

	public static void setPos(String id, OpticConfig c, int x, int y) {
		if (PEARL.equals(id)) {
			c.pearlX = x;
			c.pearlY = y;
		} else {
			c.graphX[graph(id)] = x;
			c.graphY[graph(id)] = y;
		}
	}

	public static float scale(String id, OpticConfig c) {
		return PEARL.equals(id) ? c.pearlScale : c.graphScale[graph(id)];
	}

	public static void setScale(String id, OpticConfig c, float scale) {
		float s = Math.max(0.5F, Math.min(3.0F, scale));
		if (PEARL.equals(id)) c.pearlScale = s;
		else c.graphScale[graph(id)] = s;
	}

	/** Drawn size in GUI pixels {w, h}. */
	public static int[] size(String id, OpticConfig c) {
		float s = scale(id, c);
		if (PEARL.equals(id)) return new int[] { Math.round(OpticPearl.W * s), Math.round(OpticPearl.H * s) };
		return new int[] { Math.round(OpticGraphs.W * s), Math.round(OpticGraphs.H * s) };
	}

	/** preview: the HUD editor / mod menu is open, so show widgets even when idle (pearl ready). */
	public static void renderAll(MinecraftClient client, OpticConfig c, boolean preview) {
		for (String id : enabled(c)) {
			if (PEARL.equals(id)) OpticPearl.render(client, c, preview);
			else OpticGraphs.render(client, c, graph(id));
		}
	}
}
