package gg.optic.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;

/** Hitboxes on 1.7.10. 1.7 sneaking already keeps the tall box, so it is drawn as-is. */
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
		if (client.field_3805 != null && entity == client.field_3805 && client.options.perspective == 0) return;

		Box box = entity.boundingBox;
		Box render = Box.of(
			box.minX - entity.x + x,
			box.minY - entity.y + y,
			box.minZ - entity.z + z,
			box.maxX - entity.x + x,
			box.maxY - entity.y + y,
			box.maxZ - entity.z + z
		);

		int rgb = colorFor(entity, c) & 0xFFFFFF;
		GlStateManager.depthMask(false);
		GlStateManager.disableTexture();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.disableBlend();
		GL11.glLineWidth(2.0F);
		GlStateManager.color(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, 1.0F);
		OpticBlockOverlay.drawOutline(render);

		if (!c.hitboxHideLook) {
			float eye = entity instanceof LivingEntity ? entity.getEyeHeight() : entity.height * 0.85F;
			// Same direction 1.8 gets from getRotationVector, from the interpolated yaw and pitch.
			float yaw = entity.prevYaw + (entity.yaw - entity.prevYaw) * tickDelta;
			float pitch = entity.prevPitch + (entity.pitch - entity.prevPitch) * tickDelta;
			double yawCos = Math.cos(-yaw * 0.017453292F - Math.PI);
			double yawSin = Math.sin(-yaw * 0.017453292F - Math.PI);
			double pitchCos = -Math.cos(-pitch * 0.017453292F);
			double pitchSin = Math.sin(-pitch * 0.017453292F);
			double lx = yawSin * pitchCos;
			double ly = pitchSin;
			double lz = yawCos * pitchCos;
			GlStateManager.color(0.0F, 80 / 255.0F, 1.0F, 1.0F);
			Tessellator t = Tessellator.INSTANCE;
			t.begin(GL11.GL_LINES);
			t.vertex(x, y + eye, z);
			t.vertex(x + lx * 2.0, y + eye + ly * 2.0, z + lz * 2.0);
			t.end();
		}

		GL11.glLineWidth(1.0F);
		GlStateManager.enableTexture();
		GlStateManager.enableLighting();
		GlStateManager.enableCull();
		GlStateManager.depthMask(true);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
