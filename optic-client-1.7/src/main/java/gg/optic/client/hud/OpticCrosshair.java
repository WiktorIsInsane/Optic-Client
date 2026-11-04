package gg.optic.client.hud;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.lwjgl.opengl.GL11;

/** Shaped crosshair. Replaces the vanilla cross while the module is on. */
public final class OpticCrosshair {
	public static final int SHAPE_CROSS = 0;
	public static final int SHAPE_DOT = 1;
	public static final int SHAPE_CIRCLE = 2;
	public static final int SHAPE_X = 3;

	private OpticCrosshair() {
	}

	public static boolean hidesVanilla(OpticConfig c) {
		return c.crosshair || (c.guiScale && Math.abs(sizeOf(c) - 1.0F) > 0.02F);
	}

	public static void preview(int centerX, int centerY) {
		OpticConfig c = OpticClient.get().config();
		float mul = sizeOf(c);
		paint(
			c.crosshairShape,
			centerX,
			centerY,
			c.crosshairGap * mul,
			c.crosshairLength * mul,
			Math.max(1.0F, c.crosshairThickness * mul),
			c.crosshairColor,
			c.crosshairOutline,
			c.crosshairOutlineColor
		);
	}

	public static void render(MinecraftClient client, int centerX, int centerY) {
		OpticConfig c = OpticClient.get().config();
		boolean sized = c.guiScale && Math.abs(sizeOf(c) - 1.0F) > 0.02F;
		if (!c.crosshair && !sized) return;
		if (client.field_3805 == null || client.options == null) return;
		if (client.options.debugEnabled) return;
		if (client.currentScreen != null) return;
		if (c.crosshair && client.options.perspective != 0 && !c.crosshairShowInF5) return;
		if (c.crosshair && false) return;

		float mul = sizeOf(c);
		float gap = c.crosshairGap * mul;
		float length = c.crosshairLength * mul;
		float thick = Math.max(1.0F, c.crosshairThickness * mul);
		if (!c.crosshair) {
			paint(SHAPE_CROSS, centerX, centerY, 2.0F * mul, 4.0F * mul, Math.max(1.0F, mul), 0xFFFFFF, false, 0);
			return;
		}
		if (c.crosshairDynamicAttack) {
			float swing = client.field_3805.getHandSwingProgress(1.0F);
			if (swing > 0.0F) gap += (1.0F - swing) * c.crosshairAttackSpread;
		}
		if (c.crosshairDynamicBow && client.field_3805.isUsingItem()) {
			ItemStack stack = client.field_3805.getStackInHand();
			if (stack != null && stack.getItem() == Items.BOW) {
				float use = Math.min(1.0F, client.field_3805.getItemUseTicks() / 20.0F);
				gap += use * use * c.crosshairBowSpread;
			}
		}

		int rgb = c.crosshairColor;
		if (c.crosshairHealthShift && client.field_3805.getHealth() <= c.crosshairHealthThreshold) {
			rgb = c.crosshairHealthColor;
		} else {
			LivingEntity look = client.targetedEntity instanceof LivingEntity ? (LivingEntity) client.targetedEntity : null;
			if (look != null && look != client.field_3805) {
				boolean enemy = !(look instanceof AnimalEntity);
				if (look instanceof PlayerEntity) enemy = true;
				if (enemy && c.crosshairEnemy) rgb = c.crosshairEnemyColor;
				else if (!enemy && c.crosshairFriendly) rgb = c.crosshairFriendlyColor;
			}
		}

		paint(c.crosshairShape, centerX, centerY, gap, length, thick, rgb, c.crosshairOutline, c.crosshairOutlineColor);
	}

	private static float sizeOf(OpticConfig c) {
		if (!c.guiScale) return 1.0F;
		return Math.max(0.4F, Math.min(3.0F, c.crosshairSize));
	}

	private static void paint(
		int shape,
		int centerX,
		int centerY,
		float gap,
		float length,
		float thick,
		int rgb,
		boolean outline,
		int outlineColor
	) {
		int pixels = Math.max(1, Math.round(thick));
		int reach = Math.max(1, Math.round(length));
		int hole = Math.max(0, Math.round(gap));
		int stroke = outline ? 1 : 0;
		GlStateManager.pushMatrix();
		GlStateManager.disableTexture();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.disableDepthTest();
		if (shape == SHAPE_DOT) {
			centeredBox(centerX, centerY, pixels, pixels, rgb, outlineColor, stroke);
		} else if (shape == SHAPE_CIRCLE) {
			ring(centerX, centerY, hole + reach, pixels, rgb, outlineColor, stroke);
		} else if (shape == SHAPE_X) {
			diagonal(centerX, centerY, hole, reach, pixels, 45.0F, rgb, outlineColor, stroke);
			diagonal(centerX, centerY, hole, reach, pixels, 135.0F, rgb, outlineColor, stroke);
		} else {
			// Gap is measured from the edges of the center pixel, not the center point.
			// Using the point makes the bottom and right arms one pixel closer.
			int x0 = centerX - pixels / 2;
			int y0 = centerY - pixels / 2;
			int x1 = x0 + pixels;
			int y1 = y0 + pixels;
			box(x0, y0 - hole - reach, pixels, reach, rgb, outlineColor, stroke);
			box(x0, y1 + hole, pixels, reach, rgb, outlineColor, stroke);
			box(x0 - hole - reach, y0, reach, pixels, rgb, outlineColor, stroke);
			box(x1 + hole, y0, reach, pixels, rgb, outlineColor, stroke);
		}
		GlStateManager.enableTexture();
		GlStateManager.enableDepthTest();
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	/** Outline grows the same number of pixels on every side of the fill, so it stays centered. */
	private static void box(int x, int y, int w, int h, int fill, int outline, int stroke) {
		if (w <= 0 || h <= 0) return;
		if (stroke > 0) {
			color(outline, 1.0F);
			quad(x - stroke, y - stroke, w + stroke * 2, h + stroke * 2);
		}
		color(fill, 1.0F);
		quad(x, y, w, h);
	}

	private static void centeredBox(int cx, int cy, int w, int h, int fill, int outline, int stroke) {
		box(cx - w / 2, cy - h / 2, w, h, fill, outline, stroke);
	}

	private static void ring(int cx, int cy, float radius, int thick, int fill, int outline, int stroke) {
		float mid = radius - thick / 2.0F;
		if (stroke > 0) band(cx, cy, mid, thick + stroke * 2.0F, outline);
		band(cx, cy, mid, thick, fill);
	}

	private static void band(int cx, int cy, float mid, float thick, int rgb) {
		float outer = mid + thick / 2.0F;
		float inner = Math.max(0.0F, mid - thick / 2.0F);
		color(rgb, 1.0F);
		int steps = 40;
		GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
		for (int i = 0; i <= steps; i++) {
			double ang = Math.PI * 2.0 * i / steps;
			float cos = (float) Math.cos(ang);
			float sin = (float) Math.sin(ang);
			GL11.glVertex2f(cx + cos * outer, cy + sin * outer);
			GL11.glVertex2f(cx + cos * inner, cy + sin * inner);
		}
		GL11.glEnd();
	}

	private static void diagonal(
		int cx,
		int cy,
		int hole,
		int reach,
		int thick,
		float degrees,
		int fill,
		int outline,
		int stroke
	) {
		double rad = Math.toRadians(degrees);
		float cos = (float) Math.cos(rad);
		float sin = (float) Math.sin(rad);
		float mid = hole + reach / 2.0F;
		float ox = cx + cos * mid;
		float oy = cy + sin * mid;
		if (stroke > 0) rotated(ox, oy, reach + stroke * 2.0F, thick + stroke * 2.0F, cos, sin, outline);
		rotated(ox, oy, reach, thick, cos, sin, fill);
	}

	private static void rotated(float cx, float cy, float length, float thick, float cos, float sin, int rgb) {
		float hx = cos * length / 2.0F;
		float hy = sin * length / 2.0F;
		float px = -sin * thick / 2.0F;
		float py = cos * thick / 2.0F;
		color(rgb, 1.0F);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2f(cx - hx + px, cy - hy + py);
		GL11.glVertex2f(cx + hx + px, cy + hy + py);
		GL11.glVertex2f(cx + hx - px, cy + hy - py);
		GL11.glVertex2f(cx - hx - px, cy - hy - py);
		GL11.glEnd();
	}

	private static void quad(int x, int y, int w, int h) {
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2f(x, y);
		GL11.glVertex2f(x, y + h);
		GL11.glVertex2f(x + w, y + h);
		GL11.glVertex2f(x + w, y);
		GL11.glEnd();
	}

	private static void color(int rgb, float alpha) {
		float r = ((rgb >> 16) & 255) / 255.0F;
		float g = ((rgb >> 8) & 255) / 255.0F;
		float b = (rgb & 255) / 255.0F;
		GlStateManager.color(r, g, b, alpha);
	}
}
