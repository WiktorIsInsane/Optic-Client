package gg.optic.client;

/** Resolves Time Changer / Weather Changer effective values for mixins. */
public final class OpticEnvironment {
	public static final int SKY_DEFAULT = 0;
	public static final int SKY_NETHER = 1;
	public static final int SKY_END = 2;

	public static final int PRESET_DAY = 0;
	public static final int PRESET_NOON = 1;
	public static final int PRESET_SUNSET = 2;
	public static final int PRESET_MIDNIGHT = 3;
	public static final int PRESET_CUSTOM = 4;

	public static final int WEATHER_CLEAR = 0;
	public static final int WEATHER_RAIN = 1;
	public static final int WEATHER_SNOW = 2;
	public static final int WEATHER_STORM = 3;

	private OpticEnvironment() {
	}

	public static long frozenTimeOfDay(OpticConfig c) {
		return (long) Math.max(0.0F, Math.min(23999.0F, c.timeOfDay));
	}

	public static float presetTime(int preset) {
		switch (preset) {
			case PRESET_DAY: return 1000.0F;
			case PRESET_NOON: return 6000.0F;
			case PRESET_SUNSET: return 12000.0F;
			case PRESET_MIDNIGHT: return 18000.0F;
			default: return -1.0F;
		}
	}

	public static void applyTimePreset(OpticConfig c, int preset) {
		c.timePreset = Math.max(0, Math.min(PRESET_CUSTOM, preset));
		float t = presetTime(c.timePreset);
		if (t >= 0.0F) c.timeOfDay = t;
	}

	/**
	 * The chosen time applies whenever Time Changer is on. It used to also need the separate
	 * Freeze Time toggle, which picking a preset never set, so presets fell back to the
	 * server's time (always day on most lobbies) and Midnight/Sunset never showed.
	 */
	public static boolean freezeTimeActive(OpticConfig c) {
		return c.timeChanger;
	}

	/** Client rain strength override, or -1 for vanilla. */
	public static float rainOverride(OpticConfig c) {
		if (!c.weatherChanger) return -1.0F;
		switch (c.weatherMode) {
			case WEATHER_CLEAR: return 0.0F;
			case WEATHER_RAIN:
			case WEATHER_SNOW:
			case WEATHER_STORM: return 1.0F;
			default: return -1.0F;
		}
	}

	/** Client thunder strength override, or -1 for vanilla. */
	public static float thunderOverride(OpticConfig c) {
		if (!c.weatherChanger) return -1.0F;
		if (c.weatherMode == WEATHER_STORM) return 1.0F;
		if (c.weatherMode == WEATHER_CLEAR) return 0.0F;
		return 0.0F;
	}
}
