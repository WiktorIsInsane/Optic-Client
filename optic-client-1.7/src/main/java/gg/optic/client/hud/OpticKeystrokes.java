package gg.optic.client.hud;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticHudLayout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Grouped WASD, mouse, and spacebar overlay in the Lunar keystrokes layout. */
public final class OpticKeystrokes {
	private static final float[] PRESS = new float[7];
	/** Press animation: when each key went down, and whether it was down last frame. */
	private static final long[] PRESS_START = new long[7];
	private static final boolean[] WAS_DOWN = new boolean[7];
	private static long lastFrame;

	private OpticKeystrokes() {
	}

	public static int box(OpticConfig c) {
		return Math.max(10, Math.round(c.keystrokesBoxSize * c.keystrokesScale));
	}

	public static int gap(OpticConfig c) {
		return Math.max(1, Math.round(2.0F * c.keystrokesScale));
	}

	public static int width(OpticConfig c) {
		return box(c) * 3 + gap(c) * 2;
	}

	public static int height(OpticConfig c) {
		int box = box(c);
		int gap = gap(c);
		int h = 0;
		if (c.keystrokesShowMovement) h += box * 2 + gap;
		if (c.keystrokesShowMouse) h += box + (h > 0 ? gap : 0);
		if (c.keystrokesShowSpace) h += spaceH(c) + (h > 0 ? gap : 0);
		return Math.max(box, h);
	}

	public static void render(MinecraftClient client, OpticConfig c, boolean freeze) {
		int box = box(c);
		int gap = gap(c);
		int x = c.keystrokesX;
		int y = OpticHudLayout.keystrokesBaseY(c, client);
		long now = System.currentTimeMillis();
		float dt = lastFrame == 0L ? 0.016F : Math.min(0.05F, (now - lastFrame) / 1000.0F);
		lastFrame = now;
		int row = y;
		if (c.keystrokesShowMovement) {
			String up = c.keystrokesUseArrows ? "^" : label(client.options.forwardKey, "W");
			String left = c.keystrokesUseArrows ? "<" : label(client.options.leftKey, "A");
			String down = c.keystrokesUseArrows ? "v" : label(client.options.backKey, "S");
			String right = c.keystrokesUseArrows ? ">" : label(client.options.rightKey, "D");
			key(client, c, 0, x + box + gap, row, box, box, up, movementDown(c, client.options.forwardKey, Keyboard.KEY_UP, freeze), null, dt);
			key(client, c, 1, x, row + box + gap, box, box, left, movementDown(c, client.options.leftKey, Keyboard.KEY_LEFT, freeze), null, dt);
			key(client, c, 2, x + box + gap, row + box + gap, box, box, down, movementDown(c, client.options.backKey, Keyboard.KEY_DOWN, freeze), null, dt);
			key(client, c, 3, x + (box + gap) * 2, row + box + gap, box, box, right, movementDown(c, client.options.rightKey, Keyboard.KEY_RIGHT, freeze), null, dt);
			row += box * 2 + gap * 2;
		}
		if (c.keystrokesShowMouse) {
			int mouseW = (width(c) - gap) / 2;
			String leftCps = c.keystrokesShowLeftCps ? Integer.toString(OpticClient.get().clicks().leftCps()) : null;
			String rightCps = c.keystrokesShowRightCps ? Integer.toString(OpticClient.get().clicks().rightCps()) : null;
			key(client, c, 4, x, row, mouseW, box, "LMB", bindingDown(client.options.attackKey, freeze), leftCps, dt);
			key(client, c, 5, x + mouseW + gap, row, width(c) - mouseW - gap, box, "RMB", bindingDown(client.options.useKey, freeze), rightCps, dt);
			row += box + gap;
		}
		if (c.keystrokesShowSpace) {
			key(client, c, 6, x, row, width(c), spaceH(c), "", bindingDown(client.options.jumpKey, freeze), null, dt);
		}
	}

	private static int spaceH(OpticConfig c) {
		return Math.max(8, Math.round(box(c) * 0.62F));
	}

	private static boolean movementDown(OpticConfig c, KeyBinding binding, int arrow, boolean freeze) {
		if (c.keystrokesUseArrows) return !freeze && Keyboard.isKeyDown(arrow);
		return bindingDown(binding, freeze);
	}

	private static boolean bindingDown(KeyBinding binding, boolean freeze) {
		if (freeze || binding == null) return false;
		int code = binding.getCode();
		if (code < 0) return Mouse.isButtonDown(code + 100);
		return Keyboard.isKeyDown(code);
	}

	private static String label(KeyBinding binding, String fallback) {
		if (binding == null) return fallback;
		String name = Keyboard.getKeyName(binding.getCode());
		if (name == null || name.isEmpty()) return fallback;
		return name.length() > 3 ? name.substring(0, 3) : name;
	}

	private static void key(
		MinecraftClient client,
		OpticConfig c,
		int index,
		int x,
		int y,
		int w,
		int h,
		String name,
		boolean down,
		String cps,
		float dt
	) {
		long now = System.currentTimeMillis();
		if (down && !WAS_DOWN[index]) PRESS_START[index] = now;
		WAS_DOWN[index] = down;

		float amount = PRESS[index];
		float fade = Math.max(0.0F, c.keystrokesFadeMs) / 1000.0F;
		if (!c.keystrokesFade || (c.keystrokesAnimate && down)) {
			amount = down ? 1.0F : 0.0F;
		} else if (down) {
			amount = fade <= 0.0F ? 1.0F : Math.min(1.0F, amount + dt / Math.max(0.04F, fade));
		} else {
			amount = fade <= 0.0F ? 0.0F : Math.max(0.0F, amount - dt / Math.max(0.04F, fade));
		}
		PRESS[index] = amount;

		// Both backgrounds are see-through by default (idle black, pressed white).
		int idle = OpticConfig.withAlpha(c.keystrokesBackgroundColor, 0x6F);
		int pressed = OpticConfig.withAlpha(c.keystrokesBackgroundPressedColor, 0x6F);
		float textAmount = amount;
		if (c.keystrokesAnimate) {
			OpticHud.fill(x, y, w, h, idle);
			if (down) {
				float duration = Math.max(0.1F, c.keystrokesAnimDuration) * 1000.0F;
				float p = ease(c.keystrokesEasing, Math.min(1.0F, (now - PRESS_START[index]) / duration));
				int color = c.keystrokesAnimateColor
					? blend(OpticConfig.withAlpha(c.keystrokesAnimStartColor, 0x6F), OpticConfig.withAlpha(c.keystrokesAnimEndColor, 0x6F), p)
					: pressed;
				drawPressShape(c.keystrokesAnimation, x, y, w, h, p, color);
				textAmount = p;
			} else if (amount > 0.0F) {
				OpticHud.fill(x, y, w, h, scaleAlpha(pressed, amount));
			}
		} else {
			OpticHud.fill(x, y, w, h, blend(idle, pressed, amount));
		}
		if (c.keystrokesBorder) {
			int thick = Math.max(1, Math.round(c.keystrokesBorderThickness * c.keystrokesScale));
			int border = OpticConfig.withAlpha(c.keystrokesBorderColor, 0xFF);
			// Inner border sits inside the key; otherwise it wraps around the outside.
			int bx = c.keystrokesInnerBorder ? x : x - thick;
			int by = c.keystrokesInnerBorder ? y : y - thick;
			int bw = c.keystrokesInnerBorder ? w : w + thick * 2;
			int bh = c.keystrokesInnerBorder ? h : h + thick * 2;
			OpticHud.fill(bx, by, bw, thick, border);
			OpticHud.fill(bx, by + bh - thick, bw, thick, border);
			OpticHud.fill(bx, by, thick, bh, border);
			OpticHud.fill(bx + bw - thick, by, thick, bh, border);
		}

		int text = blend(OpticConfig.argb(c.keystrokesTextColor), OpticConfig.argb(c.keystrokesTextPressedColor), textAmount);
		if (index == 6) {
			int bar = Math.max(1, Math.round(c.keystrokesSpaceThickness * c.keystrokesScale));
			int barW = Math.max(8, w - Math.round(box(c) * 0.7F));
			OpticHud.fill(x + (w - barW) / 2, y + (h - bar) / 2, barW, bar, text);
			return;
		}
		float font = OpticHudLayout.fontSize(cps == null ? c.keystrokesScale : c.keystrokesScale * 0.72F);
		if (cps == null) {
			drawCentered(client, name, x, y + (h - Math.round(font)) / 2, w, text, font, c.keystrokesTextShadow);
		} else {
			int line = Math.max(6, Math.round(font + 1.0F));
			int top = y + Math.max(1, (h - line * 2 - 1) / 2);
			drawCentered(client, name, x, top, w, text, font, c.keystrokesTextShadow);
			drawCentered(client, cps, x, top + line, w, text, font, c.keystrokesTextShadow);
		}
	}

	private static void drawCentered(MinecraftClient client, String text, int x, int y, int w, int argb, float font, boolean shadow) {
		if (text == null || text.isEmpty()) return;
		float scale = font / OpticHudLayout.BASE_FONT;
		int textW = client.textRenderer.getStringWidth(text);
		int drawX = x + (w - Math.round(textW * scale)) / 2;
		int rgb = argb & 0xFFFFFF;
		org.lwjgl.opengl.GL11.glPushMatrix();
		org.lwjgl.opengl.GL11.glTranslatef(drawX, y, 0.0F);
		org.lwjgl.opengl.GL11.glScalef(scale, scale, 1.0F);
		if (shadow) client.textRenderer.draw(text, 0, 0, rgb, true);
		else client.textRenderer.draw(text, 0, 0, rgb);
		org.lwjgl.opengl.GL11.glPopMatrix();
		org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/** 0 Linear, 1 Ease Out, 2 Ease In-Out. */
	private static float ease(int mode, float t) {
		t = Math.max(0.0F, Math.min(1.0F, t));
		if (mode == 1) return 1.0F - (1.0F - t) * (1.0F - t);
		if (mode == 2) return t < 0.5F ? 2.0F * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 2.0) / 2.0F;
		return t;
	}

	/**
	 * Pressed fill growing from the key's center: 0 Ripple (circle reaching the corners),
	 * 1 Grow (rectangle). Clipped to the key.
	 */
	private static void drawPressShape(int style, int x, int y, int w, int h, float p, int argb) {
		if (p <= 0.0F) return;
		if (style == 1) {
			int gw = Math.round(w * p);
			int gh = Math.round(h * p);
			OpticHud.fill(x + (w - gw) / 2, y + (h - gh) / 2, gw, gh, argb);
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		int sf = Math.max(1, new net.minecraft.client.util.Window(client, client.width, client.height).getScaleFactor());
		org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
		org.lwjgl.opengl.GL11.glScissor(x * sf, client.height - (y + h) * sf, w * sf, h * sf);
		float cx = x + w / 2.0F;
		float cy = y + h / 2.0F;
		float r = (float) Math.sqrt(w * w + h * h) / 2.0F * p;
		org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
		org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
		org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
		org.lwjgl.opengl.GL11.glColor4f(((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F, ((argb >>> 24) & 0xFF) / 255.0F);
		org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN);
		org.lwjgl.opengl.GL11.glVertex2f(cx, cy);
		for (int i = 0; i <= 32; i++) {
			double a = -i * Math.PI * 2.0 / 32.0;
			org.lwjgl.opengl.GL11.glVertex2f(cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r);
		}
		org.lwjgl.opengl.GL11.glEnd();
		org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
		org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
	}

	private static int scaleAlpha(int argb, float t) {
		int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0.0F, Math.min(1.0F, t)));
		return (a << 24) | (argb & 0xFFFFFF);
	}

	private static int blend(int from, int to, float t) {
		t = Math.max(0.0F, Math.min(1.0F, t));
		int af = (from >>> 24) & 0xFF;
		int rf = (from >> 16) & 0xFF;
		int gf = (from >> 8) & 0xFF;
		int bf = from & 0xFF;
		int at = (to >>> 24) & 0xFF;
		int rt = (to >> 16) & 0xFF;
		int gt = (to >> 8) & 0xFF;
		int bt = to & 0xFF;
		int a = af + Math.round((at - af) * t);
		int r = rf + Math.round((rt - rf) * t);
		int g = gf + Math.round((gt - gf) * t);
		int b = bf + Math.round((bt - bf) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
