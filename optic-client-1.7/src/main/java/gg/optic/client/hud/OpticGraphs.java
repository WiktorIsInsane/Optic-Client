package gg.optic.client.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResultType;
import org.lwjgl.opengl.GL11;

/**
 * Graphs mod: small Valorant-style performance graphs, each one its own HUD widget.
 * Samples twice a second and keeps the last 30 seconds.
 */
public final class OpticGraphs {
	public static final int COUNT = 6;
	public static final String[] NAMES = { "FPS", "Ping", "TPS", "Java", "Failed Sprints", "Missed Hits" };
	static final int W = 112;
	static final int H = 42;
	private static final int SAMPLES = 60;
	private static final int[] COLORS = { 0xFF5CFF8A, 0xFF55AAFF, 0xFFFFD166, 0xFFC77DFF, 0xFFFF8C42, 0xFFFF5566 };

	private static final float[][] HISTORY = new float[COUNT][SAMPLES];
	private static int head;
	private static int filled;
	private static int ticks;

	// TPS from the server's time packets (sent once a second).
	private static long lastServerTime = -1L;
	private static long lastServerNanos;
	private static float tps = 20.0F;
	private static ClientWorld tpsWorld;

	// Attack outcomes in the last second.
	private static final long[] SPRINT_FAILS = new long[64];
	private static final long[] MISSES = new long[64];
	private static int sprintFailHead;
	private static int missHead;

	private OpticGraphs() {
	}

	/** Every client tick. */
	public static void tick(MinecraftClient client, OpticConfig c) {
		if (!c.graphs) return;
		if (++ticks % 10 != 0) return;
		float[] now = {
			gg.optic.client.compat.Mc17.fps(),
			client.field_3805 == null ? 0 : Math.max(0, OpticPing.display(client)),
			client.world == null ? 0.0F : tps,
			javaUsage(),
			countLastSecond(SPRINT_FAILS),
			countLastSecond(MISSES)
		};
		for (int i = 0; i < COUNT; i++) HISTORY[i][head] = now[i];
		head = (head + 1) % SAMPLES;
		if (filled < SAMPLES) filled++;
	}

	/** Left click: a hit while not sprinting is a failed sprint; a swing at nothing is a miss. */
	public static void onAttack(MinecraftClient client) {
		if (client.field_3805 == null || client.currentScreen != null) return;
		BlockHitResult hit = client.result;
		long now = System.currentTimeMillis();
		if (hit != null && hit.hitResult == HitResultType.ENTITY) {
			if (!client.field_3805.isSprinting()) {
				SPRINT_FAILS[sprintFailHead] = now;
				sprintFailHead = (sprintFailHead + 1) % SPRINT_FAILS.length;
			}
		} else if (hit == null || hit.hitResult == HitResultType.MISS) {
			MISSES[missHead] = now;
			missHead = (missHead + 1) % MISSES.length;
		}
	}

	/** Server time packet: ticks the server advanced over the real time since the last one. */
	public static void onWorldTime(long serverTime) {
		MinecraftClient client = MinecraftClient.getInstance();
		long now = System.nanoTime();
		if (client.world != tpsWorld) {
			tpsWorld = client.world;
			lastServerTime = -1L;
			tps = 20.0F;
		}
		if (lastServerTime >= 0L) {
			double seconds = (now - lastServerNanos) / 1.0E9D;
			long ticksRun = serverTime - lastServerTime;
			if (seconds > 0.2D && ticksRun >= 0L) {
				float measured = (float) Math.min(20.0D, ticksRun / seconds);
				tps = tps * 0.5F + measured * 0.5F;
			}
		}
		lastServerTime = serverTime;
		lastServerNanos = now;
	}

	private static float javaUsage() {
		Runtime rt = Runtime.getRuntime();
		long used = rt.totalMemory() - rt.freeMemory();
		return rt.maxMemory() <= 0L ? 0.0F : used * 100.0F / rt.maxMemory();
	}

	private static int countLastSecond(long[] times) {
		long cutoff = System.currentTimeMillis() - 1000L;
		int n = 0;
		for (long t : times) if (t > cutoff) n++;
		return n;
	}

	private static float latest(int kind) {
		if (filled == 0) return 0.0F;
		return HISTORY[kind][(head - 1 + SAMPLES) % SAMPLES];
	}

	private static String label(int kind, float v) {
		switch (kind) {
			case 0: return Math.round(v) + "";
			case 1: return Math.round(v) + " ms";
			case 2: return String.format("%.1f", v);
			case 3: return Math.round(v) + "%";
			default: return Math.round(v) + "/s";
		}
	}

	/** Top of the chart's scale for the values on screen. */
	private static float ceiling(int kind) {
		if (kind == 2) return 20.0F;
		if (kind == 3) return 100.0F;
		float max = 0.0F;
		for (int i = 0; i < filled; i++) max = Math.max(max, HISTORY[kind][i]);
		if (kind == 0) return Math.max(60.0F, max * 1.15F);
		if (kind == 1) return Math.max(100.0F, max * 1.15F);
		return Math.max(4.0F, max + 1.0F);
	}

	static void render(MinecraftClient client, OpticConfig c, int kind) {
		float s = c.graphScale[kind];
		int color = COLORS[kind];
		GlStateManager.pushMatrix();
		GlStateManager.translate(c.graphX[kind], c.graphY[kind], 0.0F);
		GlStateManager.scale(s, s, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		DrawableHelper.fill(0, 0, W, H, 0x8C0E1014);
		DrawableHelper.fill(0, 0, 2, H, color);

		// Name on the left, current value on the right, small (Valorant's stats look).
		GlStateManager.pushMatrix();
		GlStateManager.scale(0.75F, 0.75F, 1.0F);
		client.textRenderer.draw(NAMES[kind].toUpperCase(), 7, 4, 0xFFC9CED8, true);
		String value = label(kind, latest(kind));
		int vw = client.textRenderer.getStringWidth(value);
		client.textRenderer.draw(value, Math.round((W - 4) / 0.75F) - vw, 4, 0xFFFFFFFF, true);
		GlStateManager.popMatrix();

		// Chart: faint fill under a line, newest sample on the right.
		float x0 = 5.0F;
		float y0 = 13.0F;
		float cw = W - 9.0F;
		float ch = H - 17.0F;
		DrawableHelper.fill(5, H - 4, W - 4, H - 3, 0x30FFFFFF);
		if (filled > 1) {
			float top = ceiling(kind);
			float r = ((color >> 16) & 0xFF) / 255.0F;
			float g = ((color >> 8) & 0xFF) / 255.0F;
			float b = (color & 0xFF) / 255.0F;
			GlStateManager.disableTexture();
			GL11.glShadeModel(GL11.GL_SMOOTH);
			GL11.glBegin(GL11.GL_QUAD_STRIP);
			for (int i = 0; i < filled; i++) {
				float px = x0 + cw * (SAMPLES - filled + i) / (SAMPLES - 1);
				float py = y0 + ch * (1.0F - Math.min(1.0F, sample(kind, i) / top));
				GL11.glColor4f(r, g, b, 0.28F);
				GL11.glVertex2f(px, py);
				GL11.glColor4f(r, g, b, 0.02F);
				GL11.glVertex2f(px, y0 + ch);
			}
			GL11.glEnd();
			GL11.glLineWidth(1.5F);
			GL11.glEnable(GL11.GL_LINE_SMOOTH);
			GL11.glColor4f(r, g, b, 1.0F);
			GL11.glBegin(GL11.GL_LINE_STRIP);
			for (int i = 0; i < filled; i++) {
				float px = x0 + cw * (SAMPLES - filled + i) / (SAMPLES - 1);
				float py = y0 + ch * (1.0F - Math.min(1.0F, sample(kind, i) / top));
				GL11.glVertex2f(px, py);
			}
			GL11.glEnd();
			GL11.glDisable(GL11.GL_LINE_SMOOTH);
			GL11.glLineWidth(1.0F);
			GL11.glShadeModel(GL11.GL_FLAT);
			GlStateManager.enableTexture();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		}
		GlStateManager.popMatrix();
	}

	/** i-th of the filled samples, oldest first. */
	private static float sample(int kind, int i) {
		int start = (head - filled + SAMPLES) % SAMPLES;
		return HISTORY[kind][(start + i) % SAMPLES];
	}
}
