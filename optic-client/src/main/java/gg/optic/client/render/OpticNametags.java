package gg.optic.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticUiFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.GL11;

public final class OpticNametags {
	private OpticNametags() {
	}

	public static int resolveNameColor(Entity entity, OpticConfig c) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null && entity == client.player) {
			return c.nametagsPersonalColor & 0xFFFFFF;
		}
		String name = playerName(entity);
		if (name != null) {
			Integer custom = c.nametagNameColors.get(name);
			if (custom != null) return custom & 0xFFFFFF;
		}
		return 0xFFFFFF;
	}

	public static String playerName(Entity entity) {
		if (entity instanceof AbstractClientPlayerEntity) {
			return ((AbstractClientPlayerEntity) entity).getGameProfile().getName();
		}
		if (entity instanceof PlayerEntity) {
			return entity.getName().asFormattedString();
		}
		return null;
	}

	public static void render(
		EntityRenderDispatcher dispatcher,
		Entity entity,
		String text,
		double x,
		double y,
		double z,
		int maxDistance
	) {
		OpticConfig c = OpticClient.get().config();
		MinecraftClient client = MinecraftClient.getInstance();
		Entity camera = client.getCameraEntity();
		if (camera == null) camera = client.player;
		if (camera == null) return;

		double dist = entity.squaredDistanceTo(camera);
		if (dist > (double) maxDistance * (double) maxDistance) return;

		TextRenderer textRenderer = dispatcher.getTextRenderer();
		float scale = 0.016666668F * 1.6F;
		GlStateManager.pushMatrix();
		GlStateManager.translate((float) x, (float) y + entity.height + 0.5F, (float) z);
		GL11.glNormal3f(0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(-dispatcher.yaw, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(dispatcher.pitch, 1.0F, 0.0F, 0.0F);
		GlStateManager.scale(-scale, -scale, scale);
		// Front F5 adds a 180° view flip, which mirrors the tag. Un-mirror so it
		// reads the same as when you are looking at the back of the head.
		if (client.options != null && client.options.perspective == 2) {
			GlStateManager.scale(-1.0F, 1.0F, 1.0F);
		}
		GlStateManager.disableLighting();
		GlStateManager.depthMask(false);
		GlStateManager.disableDepthTest();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);

		int yOff = "deadmau5".equals(text) ? -10 : 0;
		int nameRgb = resolveNameColor(entity, c);
		int nameArgb = 0xFF000000 | nameRgb;

		int halfW;
		if (c.nametagsBadlionFont) {
			halfW = Math.max(1, OpticUiFont.widthSized(client, text, nameArgb, 8.0F) / 2);
		} else {
			halfW = textRenderer.getStringWidth(text) / 2;
		}

		if (c.nametagsBackground) {
			float[] bg = rgba(c.nametagsBackgroundColor, 0.25F);
			GlStateManager.disableTexture();
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder buf = tessellator.getBuffer();
			buf.begin(7, VertexFormats.POSITION_COLOR);
			if (c.nametagsRoundedCorners) {
				drawRoundedBackground(buf, -halfW - 1, -1 + yOff, halfW + 1, 8 + yOff, 2, bg);
			} else {
				buf.vertex(-halfW - 1, -1 + yOff, 0.0).color(bg[0], bg[1], bg[2], bg[3]).next();
				buf.vertex(-halfW - 1, 8 + yOff, 0.0).color(bg[0], bg[1], bg[2], bg[3]).next();
				buf.vertex(halfW + 1, 8 + yOff, 0.0).color(bg[0], bg[1], bg[2], bg[3]).next();
				buf.vertex(halfW + 1, -1 + yOff, 0.0).color(bg[0], bg[1], bg[2], bg[3]).next();
			}
			tessellator.draw();
			GlStateManager.enableTexture();
		}

		if (c.nametagsBadlionFont) {
			int drawX = -halfW;
			if (c.nametagsTextShadow) {
				OpticUiFont.drawSized(client, text, drawX + 1, yOff + 1, 0xFF000000, 8.0F);
			}
			OpticUiFont.drawSized(client, text, drawX, yOff, nameArgb, 8.0F);
		} else {
			float drawX = -textRenderer.getStringWidth(text) / 2.0F;
			if (c.nametagsTextShadow) {
				textRenderer.drawWithShadow(text, drawX, (float) yOff, nameArgb);
			} else {
				textRenderer.draw(text, (int) drawX, yOff, nameArgb);
			}
		}

		GlStateManager.enableDepthTest();
		GlStateManager.depthMask(true);
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	private static void drawRoundedBackground(
		BufferBuilder buf,
		int left,
		int top,
		int right,
		int bottom,
		int radius,
		float[] bg
	) {
		int r = Math.max(1, Math.min(radius, Math.min((right - left) / 2, (bottom - top) / 2)));
		quad(buf, left + r, top, right - r, bottom, bg);
		quad(buf, left, top + r, left + r, bottom - r, bg);
		quad(buf, right - r, top + r, right, bottom - r, bg);
		quad(buf, left + r, top, left + r + r, top + r, bg);
		quad(buf, right - r - r, top, right - r, top + r, bg);
		quad(buf, left + r, bottom - r, left + r + r, bottom, bg);
		quad(buf, right - r - r, bottom - r, right - r, bottom, bg);
	}

	private static void quad(BufferBuilder buf, int left, int top, int right, int bottom, float[] bg) {
		if (right <= left || bottom <= top) return;
		buf.vertex(left, top, 0.0).color(bg[0], bg[1], bg[2], bg[3]).next();
		buf.vertex(left, bottom, 0.0).color(bg[0], bg[1], bg[2], bg[3]).next();
		buf.vertex(right, bottom, 0.0).color(bg[0], bg[1], bg[2], bg[3]).next();
		buf.vertex(right, top, 0.0).color(bg[0], bg[1], bg[2], bg[3]).next();
	}

	private static float[] rgba(int rgb, float alpha) {
		return new float[] {
			((rgb >> 16) & 0xFF) / 255.0F,
			((rgb >> 8) & 0xFF) / 255.0F,
			(rgb & 0xFF) / 255.0F,
			alpha
		};
	}
}
