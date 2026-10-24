package gg.optic.client;

/** Client-only record of the last entity this player swung at, used to hide that hit's particles. */
public final class OpticOwnHit {
	private static double x;
	private static double y;
	private static double z;
	private static long at;

	private OpticOwnHit() {
	}

	public static void mark(double hitX, double hitY, double hitZ) {
		x = hitX;
		y = hitY;
		z = hitZ;
		at = System.currentTimeMillis();
	}

	public static boolean near(double px, double py, double pz) {
		if (at == 0L || System.currentTimeMillis() - at > 500L) return false;
		double dx = px - x;
		double dy = py - y;
		double dz = pz - z;
		return dx * dx + dy * dy + dz * dz <= 6.25D;
	}
}
