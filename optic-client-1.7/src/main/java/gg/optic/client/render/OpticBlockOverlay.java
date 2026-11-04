package gg.optic.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticConfig;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResultType;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;

/** Block Overlay on 1.7.10: same fill and outline as 1.8.9, using 1.7's x/y/z block lookups. */
public final class OpticBlockOverlay {
	private OpticBlockOverlay() {
	}

	public static void draw(ClientWorld world, PlayerEntity player, BlockHitResult hit, float tickDelta, OpticConfig c) {
		if (hit.hitResult != HitResultType.BLOCK) return;
		Block block = world.getBlock(hit.x, hit.y, hit.z);
		if (block == null || block.getMaterial() == Material.AIR) return;

		block.onRender(world, hit.x, hit.y, hit.z);
		Box box = block.getRenderBoundingBox(world, hit.x, hit.y, hit.z);
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
			boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
			if (c.blockOverlay3d) GlStateManager.disableDepthTest();
			drawFilled(expanded, rgba[0], rgba[1], rgba[2], rgba[3]);
			if (c.blockOverlay3d && depth) GlStateManager.enableDepthTest();
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
		Tessellator t = Tessellator.INSTANCE;
		t.begin(GL11.GL_QUADS);
		t.color(r, g, b, a);
		// bottom
		t.vertex(box.minX, box.minY, box.minZ);
		t.vertex(box.maxX, box.minY, box.minZ);
		t.vertex(box.maxX, box.minY, box.maxZ);
		t.vertex(box.minX, box.minY, box.maxZ);
		// top
		t.vertex(box.minX, box.maxY, box.minZ);
		t.vertex(box.minX, box.maxY, box.maxZ);
		t.vertex(box.maxX, box.maxY, box.maxZ);
		t.vertex(box.maxX, box.maxY, box.minZ);
		// north
		t.vertex(box.minX, box.minY, box.minZ);
		t.vertex(box.minX, box.maxY, box.minZ);
		t.vertex(box.maxX, box.maxY, box.minZ);
		t.vertex(box.maxX, box.minY, box.minZ);
		// south
		t.vertex(box.minX, box.minY, box.maxZ);
		t.vertex(box.maxX, box.minY, box.maxZ);
		t.vertex(box.maxX, box.maxY, box.maxZ);
		t.vertex(box.minX, box.maxY, box.maxZ);
		// west
		t.vertex(box.minX, box.minY, box.minZ);
		t.vertex(box.minX, box.minY, box.maxZ);
		t.vertex(box.minX, box.maxY, box.maxZ);
		t.vertex(box.minX, box.maxY, box.minZ);
		// east
		t.vertex(box.maxX, box.minY, box.minZ);
		t.vertex(box.maxX, box.maxY, box.minZ);
		t.vertex(box.maxX, box.maxY, box.maxZ);
		t.vertex(box.maxX, box.minY, box.maxZ);
		t.end();
	}

	/** Twelve edges as GL lines; colour comes from the current GL colour. */
	public static void drawOutline(Box box) {
		Tessellator t = Tessellator.INSTANCE;
		t.begin(GL11.GL_LINE_STRIP);
		t.vertex(box.minX, box.minY, box.minZ);
		t.vertex(box.maxX, box.minY, box.minZ);
		t.vertex(box.maxX, box.minY, box.maxZ);
		t.vertex(box.minX, box.minY, box.maxZ);
		t.vertex(box.minX, box.minY, box.minZ);
		t.end();
		t.begin(GL11.GL_LINE_STRIP);
		t.vertex(box.minX, box.maxY, box.minZ);
		t.vertex(box.maxX, box.maxY, box.minZ);
		t.vertex(box.maxX, box.maxY, box.maxZ);
		t.vertex(box.minX, box.maxY, box.maxZ);
		t.vertex(box.minX, box.maxY, box.minZ);
		t.end();
		t.begin(GL11.GL_LINES);
		t.vertex(box.minX, box.minY, box.minZ);
		t.vertex(box.minX, box.maxY, box.minZ);
		t.vertex(box.maxX, box.minY, box.minZ);
		t.vertex(box.maxX, box.maxY, box.minZ);
		t.vertex(box.maxX, box.minY, box.maxZ);
		t.vertex(box.maxX, box.maxY, box.maxZ);
		t.vertex(box.minX, box.minY, box.maxZ);
		t.vertex(box.minX, box.maxY, box.maxZ);
		t.end();
	}
}
