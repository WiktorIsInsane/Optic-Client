package gg.optic.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticConfig;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;

public final class OpticBlockOverlay {
	private OpticBlockOverlay() {
	}

	public static void draw(ClientWorld world, PlayerEntity player, BlockHitResult hit, float tickDelta, OpticConfig c) {
		if (hit.type != BlockHitResult.Type.BLOCK) return;
		BlockPos blockPos = hit.getBlockPos();
		Block block = world.getBlockState(blockPos).getBlock();
		if (block.getMaterial() == Material.AIR || !world.getWorldBorder().contains(blockPos)) return;

		block.setBoundingBox(world, blockPos);
		Box box = block.getSelectionBox(world, blockPos);
		if (box == null) return;

		double dx = player.prevTickX + (player.x - player.prevTickX) * tickDelta;
		double dy = player.prevTickY + (player.y - player.prevTickY) * tickDelta;
		double dz = player.prevTickZ + (player.z - player.prevTickZ) * tickDelta;
		Box expanded = box.expand(0.002F, 0.002F, 0.002F).offset(-dx, -dy, -dz);

		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GlStateManager.disableTexture();
		GlStateManager.depthMask(false);
		GlStateManager.disableLighting();

		if (c.blockOverlayFill) {
			float[] rgba = rgba(c.blockOverlayFillColor, c.blockOverlayFillAlpha);
			drawFilled(expanded, rgba[0], rgba[1], rgba[2], rgba[3]);
		}

		if (c.blockOverlayOutline) {
			float[] rgba = rgba(c.blockOverlayOutlineColor, c.blockOverlayOutlineAlpha);
			GL11.glLineWidth(Math.max(1.0F, c.blockOverlayLineWidth));
			GlStateManager.color(rgba[0], rgba[1], rgba[2], rgba[3]);
			drawOutline(expanded);
		}

		GlStateManager.depthMask(true);
		GlStateManager.enableTexture();
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glLineWidth(1.0F);
	}

	private static float[] rgba(int rgb, float alpha) {
		return new float[] {
			((rgb >> 16) & 0xFF) / 255.0F,
			((rgb >> 8) & 0xFF) / 255.0F,
			(rgb & 0xFF) / 255.0F,
			Math.max(0.0F, Math.min(1.0F, alpha))
		};
	}

	private static void drawFilled(Box box, float r, float g, float b, float a) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buf = tessellator.getBuffer();
		buf.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
		// bottom
		buf.vertex(box.minX, box.minY, box.minZ).color(r, g, b, a).next();
		buf.vertex(box.maxX, box.minY, box.minZ).color(r, g, b, a).next();
		buf.vertex(box.maxX, box.minY, box.maxZ).color(r, g, b, a).next();
		buf.vertex(box.minX, box.minY, box.maxZ).color(r, g, b, a).next();
		// top
		buf.vertex(box.minX, box.maxY, box.minZ).color(r, g, b, a).next();
		buf.vertex(box.minX, box.maxY, box.maxZ).color(r, g, b, a).next();
		buf.vertex(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).next();
		buf.vertex(box.maxX, box.maxY, box.minZ).color(r, g, b, a).next();
		// north
		buf.vertex(box.minX, box.minY, box.minZ).color(r, g, b, a).next();
		buf.vertex(box.minX, box.maxY, box.minZ).color(r, g, b, a).next();
		buf.vertex(box.maxX, box.maxY, box.minZ).color(r, g, b, a).next();
		buf.vertex(box.maxX, box.minY, box.minZ).color(r, g, b, a).next();
		// south
		buf.vertex(box.minX, box.minY, box.maxZ).color(r, g, b, a).next();
		buf.vertex(box.maxX, box.minY, box.maxZ).color(r, g, b, a).next();
		buf.vertex(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).next();
		buf.vertex(box.minX, box.maxY, box.maxZ).color(r, g, b, a).next();
		// west
		buf.vertex(box.minX, box.minY, box.minZ).color(r, g, b, a).next();
		buf.vertex(box.minX, box.minY, box.maxZ).color(r, g, b, a).next();
		buf.vertex(box.minX, box.maxY, box.maxZ).color(r, g, b, a).next();
		buf.vertex(box.minX, box.maxY, box.minZ).color(r, g, b, a).next();
		// east
		buf.vertex(box.maxX, box.minY, box.minZ).color(r, g, b, a).next();
		buf.vertex(box.maxX, box.maxY, box.minZ).color(r, g, b, a).next();
		buf.vertex(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).next();
		buf.vertex(box.maxX, box.minY, box.maxZ).color(r, g, b, a).next();
		tessellator.draw();
	}

	private static void drawOutline(Box box) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buf = tessellator.getBuffer();
		buf.begin(3, VertexFormats.POSITION);
		buf.vertex(box.minX, box.minY, box.minZ).next();
		buf.vertex(box.maxX, box.minY, box.minZ).next();
		buf.vertex(box.maxX, box.minY, box.maxZ).next();
		buf.vertex(box.minX, box.minY, box.maxZ).next();
		buf.vertex(box.minX, box.minY, box.minZ).next();
		tessellator.draw();
		buf.begin(3, VertexFormats.POSITION);
		buf.vertex(box.minX, box.maxY, box.minZ).next();
		buf.vertex(box.maxX, box.maxY, box.minZ).next();
		buf.vertex(box.maxX, box.maxY, box.maxZ).next();
		buf.vertex(box.minX, box.maxY, box.maxZ).next();
		buf.vertex(box.minX, box.maxY, box.minZ).next();
		tessellator.draw();
		buf.begin(1, VertexFormats.POSITION);
		buf.vertex(box.minX, box.minY, box.minZ).next();
		buf.vertex(box.minX, box.maxY, box.minZ).next();
		buf.vertex(box.maxX, box.minY, box.minZ).next();
		buf.vertex(box.maxX, box.maxY, box.minZ).next();
		buf.vertex(box.maxX, box.minY, box.maxZ).next();
		buf.vertex(box.maxX, box.maxY, box.maxZ).next();
		buf.vertex(box.minX, box.minY, box.maxZ).next();
		buf.vertex(box.minX, box.maxY, box.maxZ).next();
		tessellator.draw();
	}
}
