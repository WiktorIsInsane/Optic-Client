package gg.optic.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public final class OpticHitboxes {
	private OpticHitboxes() {
	}

	public static int colorFor(Entity entity, OpticConfig c) {
		if (entity instanceof AbstractArrowEntity) return c.hitboxArrowColor;
		if (entity instanceof PlayerEntity) return c.hitboxPlayerColor;
		if (entity instanceof MobEntity) return c.hitboxMobColor;
		return c.hitboxOtherColor;
	}

	public static void draw(Entity entity, double x, double y, double z, float tickDelta, OpticConfig c) {
		if (entity == null || entity.isInvisible()) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null && entity == client.player && client.options.perspective == 0) return;

		Box box = entity.getBoundingBox();
		double maxY = box.maxY;
		// 1.7 sneaking players kept a 1.65-tall box. 1.8 uses 1.5. Drawn outline only.
		if (entity instanceof PlayerEntity
			&& entity.isSneaking()
			&& c.anim(c.animHitbox)
			&& maxY - box.minY < 1.65D) {
			maxY = box.minY + 1.65D;
		}
		Box render = new Box(
			box.minX - entity.x + x,
			box.minY - entity.y + y,
			box.minZ - entity.z + z,
			box.maxX - entity.x + x,
			maxY - entity.y + y,
			box.maxZ - entity.z + z
		);

		int rgb = colorFor(entity, c) & 0xFFFFFF;
		int red = (rgb >> 16) & 0xFF;
		int green = (rgb >> 8) & 0xFF;
		int blue = rgb & 0xFF;

		GlStateManager.depthMask(false);
		GlStateManager.disableTexture();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.disableBlend();
		GL11.glLineWidth(2.0F);
		WorldRenderer.drawBox(render, red, green, blue, 255);

		if (!c.hitboxHideLook) {
			float eye = entity instanceof LivingEntity ? entity.getEyeHeight() : entity.height * 0.85F;
			Vec3d look = entity.getRotationVector(tickDelta);
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder buf = tessellator.getBuffer();
			buf.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
			buf.vertex(x, y + eye, z).color(0, 80, 255, 255).next();
			buf.vertex(x + look.x * 2.0, y + eye + look.y * 2.0, z + look.z * 2.0).color(0, 80, 255, 255).next();
			tessellator.draw();
		}

		GL11.glLineWidth(1.0F);
		GlStateManager.enableTexture();
		GlStateManager.enableLighting();
		GlStateManager.enableCull();
		GlStateManager.depthMask(true);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
