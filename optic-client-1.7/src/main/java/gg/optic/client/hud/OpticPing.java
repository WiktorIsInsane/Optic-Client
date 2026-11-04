package gg.optic.client.hud;

import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.class_482;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;

/** Latency sample, spike marker, and the color bands used by the ping HUD and tab list. */
public final class OpticPing {
	private static int previous = -1;
	private static int spikePercent;
	private static long spikeUntil;

	/** Status-ping fallback for servers whose tab list never carries our own name. */
	private static final long STATUS_INTERVAL_MS = 10000L;
	private static final MultiplayerServerListPinger PINGER = new MultiplayerServerListPinger();
	private static volatile ServerInfo pending;
	private static volatile boolean pinging;
	private static long nextStatusPing;
	private static int statusPing = -1;
	private static String statusAddress;

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
		if (client == null || client.field_3805 == null) return -1;
		if (client.isInSingleplayer()) return 0;
		int tab = readName(client, client.field_3805.getGameProfile().getName());
		if (tab > 0) return tab;
		int status = statusPing(client);
		if (status >= 0) return status;
		return tab;
	}

	/**
	 * 1.7 keeps ping only in the tab list, keyed by the name the server sent. Many servers
	 * (and 1.8+ servers seen through a protocol translator) send a coloured or prefixed name,
	 * so fall back to a formatting-stripped, case-insensitive match.
	 */
	public static int readName(MinecraftClient client, String name) {
		if (client == null || name == null || client.getNetworkHandler() == null) return -1;
		java.util.List<?> list = client.getNetworkHandler().field_7911;
		if (list == null) return -1;
		class_482 loose = null;
		for (Object item : list) {
			if (!(item instanceof class_482)) continue;
			class_482 entry = (class_482) item;
			if (entry.field_1679 == null) continue;
			if (name.equals(entry.field_1679)) return Math.max(0, entry.field_1680);
			if (loose != null) continue;
			String plain = strip(entry.field_1679);
			if (plain.equalsIgnoreCase(name) || endsWithName(plain, name)) loose = entry;
		}
		return loose == null ? -1 : Math.max(0, loose.field_1680);
	}

	private static boolean endsWithName(String plain, String name) {
		if (plain.length() <= name.length()) return false;
		if (!plain.toLowerCase().endsWith(name.toLowerCase())) return false;
		char before = plain.charAt(plain.length() - name.length() - 1);
		return !Character.isLetterOrDigit(before) && before != '_';
	}

	private static String strip(String text) {
		String plain = Formatting.strip(text);
		return plain == null ? "" : plain.trim();
	}

	/** Latency from a background server-list ping, refreshed every few seconds. */
	private static int statusPing(MinecraftClient client) {
		ServerInfo current = client.getCurrentServerEntry();
		if (current == null || current.address == null) return -1;
		if (!current.address.equals(statusAddress)) {
			statusAddress = current.address;
			statusPing = current.ping > 0 ? (int) current.ping : -1;
			nextStatusPing = 0L;
		}
		try {
			PINGER.tick();
		} catch (Throwable ignored) {
		}
		ServerInfo done = pending;
		if (done != null && !pinging && done.ping > 0 && done.address.equals(statusAddress)) {
			statusPing = (int) done.ping;
			pending = null;
		}
		long now = System.currentTimeMillis();
		if (!pinging && now >= nextStatusPing) {
			nextStatusPing = now + STATUS_INTERVAL_MS;
			final ServerInfo probe = new ServerInfo(current.name, current.address);
			probe.ping = -1L;
			pending = probe;
			pinging = true;
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						PINGER.add(probe);
					} catch (Throwable ignored) {
						probe.ping = -1L;
					} finally {
						pinging = false;
					}
				}
			}, "Optic ping");
			thread.setDaemon(true);
			thread.start();
		}
		return statusPing;
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
		if (client.field_3805 != null && player == client.field_3805) ping = read(client);
		else if (client.getNetworkHandler() != null) {
			ping = readName(client, player.getGameProfile().getName());
		} else {
			return "";
		}
		if (ping < 0) return "";
		String number = Integer.toString(ping) + (c.pingShowMs ? "ms" : "");
		return " " + number;
	}
}
