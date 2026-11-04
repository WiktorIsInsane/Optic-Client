package gg.optic.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerInfo;

/** Remembers the last multiplayer server so DisconnectedScreen can reconnect. */
public final class OpticReconnect {
	private static ServerInfo lastServer;
	private static Screen lastParent;

	private OpticReconnect() {
	}

	public static void remember(Screen parent, ServerInfo server) {
		if (server == null) return;
		lastParent = parent;
		lastServer = new ServerInfo(server.name, server.address, server.isLocal());
	}

	public static ServerInfo lastServer() {
		return lastServer;
	}

	public static Screen lastParent() {
		return lastParent;
	}

	public static boolean canReconnect() {
		return lastServer != null && lastServer.address != null && !lastServer.address.isEmpty();
	}
}
