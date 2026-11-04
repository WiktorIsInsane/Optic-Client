package gg.optic.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * Optic's client bootstrap. Features are deliberately client-side and
 * cosmetic so the client remains safe for competitive 1.8.9 servers.
 */
public final class OpticClient implements ClientModInitializer {
	public static final String NAME = "Optic";
	public static final String VERSION = "1.0.0";
	public static final String MINECRAFT_VERSION = "1.7.10";
	public static final int GREEN = 0xFF66CC33;

	private static final OpticClient INSTANCE = new OpticClient();

	private final OpticConfig config = new OpticConfig();
	private final ClickTracker clicks = new ClickTracker();

	public static OpticClient get() {
		return INSTANCE;
	}

	public OpticConfig config() {
		return config;
	}

	public ClickTracker clicks() {
		return clicks;
	}

	@Override
	public void onInitializeClient() {
		config.load();
		OpticPerf.sync(config);
		OpticTimer.begin();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					config.save();
				} catch (Throwable ignored) {
				}
			}
		}, "optic-profile-save"));
		System.setProperty("optic.version", VERSION);
		System.out.println("[Optic] " + VERSION + " initialized for Minecraft " + MINECRAFT_VERSION);
	}
}
