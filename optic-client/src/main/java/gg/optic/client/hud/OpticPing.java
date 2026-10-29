package gg.optic.client.hud;

import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;

/** Latency sample, spike marker, and the color bands used by the ping HUD and tab list. */
public final class OpticPing {
	private static int previous = -1;
	private static int spikePercent;
	private static long spikeUntil;

	private OpticPing() {
	}

	public static int display(MinecraftClient client) {
		int ping = read(client);
		long now = System.currentTimeMillis();
		if (previous >= 0 && ping >= 0 && previous > 0) {
			int delta = (int) ((ping - previous) / (float) previous * 100.0F);
			OpticConfig c = gg.optic.client.OpticClient.get().config();
			if (c.pingSpike && Math.abs(delta) >= c.pingSpikeThreshold) {
				spikePercent = delta;
				spikeUntil = now + 5000L;
			}
		}
		if (ping >= 0) previous = ping;
		if (now > spikeUntil) spikePercent = 0;
		return ping;
	}

	public static int read(MinecraftClient client) {
		if (client == null || client.player == null || client.getNetworkHandler() == null) return -1;
		PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
		if (entry == null) return -1;
		return Math.max(0, entry.getLatency());
	}

	public static int read(PlayerListEntry entry) {
		if (entry == null) return -1;
		return Math.max(0, entry.getLatency());
	}

	public static String spikeText() {
		if (spikePercent == 0 || System.currentTimeMillis() > spikeUntil) return "";
		if (!gg.optic.client.OpticClient.get().config().pingSpike) return "";
		return " (" + (spikePercent > 0 ? "+" : "") + spikePercent + "%)";
	}

	public static int colorFor(OpticConfig c, int ping) {
		if (ping < 0) return c.pingTextColor;
		if (ping < 65) return c.pingLowColor;
		if (ping < 120) return c.pingMediumColor;
		if (ping < 250) return c.pingHighColor;
		return c.pingExtremeColor;
	}

	public static String nametagSuffix(PlayerEntity player) {
		OpticConfig c = gg.optic.client.OpticClient.get().config();
		if (!c.ping || !c.pingNametag || player == null) return "";
		MinecraftClient client = MinecraftClient.getInstance();
		int ping;
		if (client.player != null && player == client.player) ping = read(client);
		else if (client.getNetworkHandler() != null) {
			PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
			ping = read(entry);
		} else {
			return "";
		}
		if (ping < 0) return "";
		String number = Integer.toString(ping) + (c.pingShowMs ? "ms" : "");
		return " " + number;
	}
}
