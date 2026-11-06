package gg.optic.client.cosmetics;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.BiPedModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.model.ModelPart;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * The cosmetics Optic can draw: the Optic cape and ender dragon wings. Everything here works
 * in player model space (y down, 1 unit = 16 px, origin at the neck, back facing +z), which is
 * where the player renderer draws its extras and where the Cosmetics preview draws its model.
 */
public final class OpticCosmetics {
	public static final String CAPE_OPTIC = "optic";
	public static final String WINGS_DRAGON = "ender_dragon";

	private static final Identifier DRAGON = new Identifier("textures/entity/enderdragon/dragon.png");
	private static final BiPedModel CAPE_MODEL = new BiPedModel(0.0F);
	private static final WingsModel WINGS = new WingsModel();
	private static Identifier capeId;
	private static boolean capeTried;
	private static long capeTriedAt;

	private OpticCosmetics() {
	}

	public static boolean capeEquipped() {
		return CAPE_OPTIC.equals(SharedCosmetics.equipped("capes"));
	}

	public static boolean wingsEquipped() {
		return WINGS_DRAGON.equals(SharedCosmetics.equipped("wings"));
	}

	/** Cosmetics only show on this player: there is no server telling us what others wear. */
	public static boolean isLocal(PlayerEntity player) {
		MinecraftClient client = MinecraftClient.getInstance();
		return client != null && player != null && player == client.field_3805;
	}

	// Cape angles, eased every frame. Vanilla only updates its cape trail 20 times a second,
	// which is what made the cape step instead of flow.
	private static float capePitch = 6.0F;
	private static float capeSide;
	private static long capeLastNanos;

	/** Cape that swings back with speed, lifts when falling and sways side to side. Model space. */
	public static void renderCape(PlayerEntity p, float tickDelta) {
		// Movement over the last tick, in blocks per tick, relative to where the body faces.
		double vx = p.x - p.prevX;
		double vy = p.y - p.prevY;
		double vz = p.z - p.prevZ;
		float bodyYaw = p.prevBodyYaw + (p.bodyYaw - p.prevBodyYaw) * tickDelta;
		double fx = -Math.sin(Math.toRadians(bodyYaw));
		double fz = Math.cos(Math.toRadians(bodyYaw));
		float forward = (float) (vx * fx + vz * fz);
		float strafe = (float) (vx * fz - vz * fx);

		// Same feel as vanilla at steady speed (walking ~ +30 degrees, sprinting ~ +45).
		float targetPitch = 6.0F + Math.max(0.0F, forward) * 150.0F;
		targetPitch += Math.max(-6.0F, Math.min(32.0F, (float) -vy * 30.0F));
		float speed = (float) Math.sqrt(vx * vx + vz * vz);
		targetPitch += (float) Math.sin(System.currentTimeMillis() / 90.0) * Math.min(1.0F, speed * 5.0F) * 4.0F;
		if (p.isSneaking()) targetPitch += 25.0F;
		targetPitch = Math.min(100.0F, targetPitch);
		float targetSide = Math.max(-40.0F, Math.min(40.0F, strafe * 300.0F));

		long now = System.nanoTime();
		float dt = capeLastNanos == 0L ? 1.0F : Math.min(0.25F, (now - capeLastNanos) / 1.0E9F);
		capeLastNanos = now;
		float k = 1.0F - (float) Math.exp(-dt * 9.0F);
		capePitch += (targetPitch - capePitch) * k;
		capeSide += (targetSide - capeSide) * k;
		drawCape(capePitch, capeSide);
	}

	/** Cape hanging off the back: pitch lifts it away from the body, side swings it. */
	public static void drawCape(float pitch, float side) {
		Identifier tex = capeTexture();
		if (tex == null) return;
		MinecraftClient.getInstance().getTextureManager().bindTexture(tex);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.pushMatrix();
		GlStateManager.translate(0.0F, 0.0F, 0.125F);
		GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
		GlStateManager.rotate(side / 2.0F, 0.0F, 0.0F, 1.0F);
		GlStateManager.rotate(-side / 2.0F, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
		CAPE_MODEL.field_1481.render(0.0625F);
		GlStateManager.popMatrix();
	}

	/** Two dragon wings flapping slowly off the upper back. Model space. */
	public static void renderWings(boolean sneaking) {
		MinecraftClient.getInstance().getTextureManager().bindTexture(DRAGON);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.disableCull();
		GlStateManager.pushMatrix();
		if (sneaking) GlStateManager.rotate(28.6F, 1.0F, 0.0F, 0.0F);
		GlStateManager.translate(0.0F, 0.18F, 0.14F);
		float t = (System.currentTimeMillis() % 1600L) / 1600.0F * (float) Math.PI * 2.0F;
		WINGS.pose(t);
		// The dragon's wing is ~7 blocks long; shrink it to about 0.9 blocks per side.
		float s = 0.0625F * 0.13F;
		for (int side = 0; side < 2; side++) {
			WINGS.wing.render(s);
			GlStateManager.scale(-1.0F, 1.0F, 1.0F);
		}
		GlStateManager.popMatrix();
		GlStateManager.enableCull();
	}

	/** 64x32 cape layout at 8x resolution: black, with the Optic logo on both faces. */
	public static Identifier capeTexture() {
		if (capeId != null) {
			// If the GL texture behind it is gone (a resource reload, some drivers), whatever was
			// bound last (the skin) would be drawn on the cape. Rebuild it instead.
			net.minecraft.client.texture.Texture t = MinecraftClient.getInstance().getTextureManager().getTexture(capeId);
			if (t != null && GL11.glIsTexture(t.getGlId())) return capeId;
			capeId = null;
			capeTried = false;
		}
		// A failed build is retried every few seconds rather than never.
		if (capeTried && System.currentTimeMillis() - capeTriedAt < 5000L) return null;
		capeTried = true;
		capeTriedAt = System.currentTimeMillis();
		try {
			int k = 8;
			BufferedImage cape = new BufferedImage(64 * k, 32 * k, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = cape.createGraphics();
			g.setColor(new java.awt.Color(0x0B0B0D));
			g.fillRect(0, 0, 22 * k, 17 * k);
			BufferedImage logo = null;
			InputStream in = OpticCosmetics.class.getResourceAsStream("/assets/optic/textures/gui/logo.png");
			if (in != null) {
				try {
					logo = ImageIO.read(in);
				} finally {
					in.close();
				}
			}
			if (logo != null) {
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				int size = 8 * k;
				// Outer face (1,1)-(11,17) and inner face (12,1)-(22,17), logo in the upper middle.
				g.drawImage(logo, 1 * k + (10 * k - size) / 2, 1 * k + 3 * k, size, size, null);
				g.drawImage(logo, 12 * k + (10 * k - size) / 2, 1 * k + 3 * k, size, size, null);
			}
			g.dispose();
			NativeImageBackedTexture texture = new NativeImageBackedTexture(cape);
			capeId = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("optic_cape", texture);
		} catch (Exception e) {
			System.err.println("[Optic] Cape texture failed: " + e.getMessage());
		}
		return capeId;
	}

	/** The ender dragon's wing (bone + membrane, with its tip), using the dragon texture. */
	private static final class WingsModel extends EntityModel {
		final ModelPart wing;
		final ModelPart tip;

		WingsModel() {
			textureWidth = 256;
			textureHeight = 256;
			wing = new ModelPart(this, 112, 88);
			wing.setPivot(-16.0F, 0.0F, 0.0F);
			wing.addCuboid(-56.0F, -4.0F, -4.0F, 56, 8, 8);
			ModelPart skin = new ModelPart(this, -56, 88);
			skin.addCuboid(-56.0F, 0.0F, 2.0F, 56, 0, 56);
			wing.add(skin);
			tip = new ModelPart(this, 112, 136);
			tip.setPivot(-56.0F, 0.0F, 0.0F);
			tip.addCuboid(-56.0F, -2.0F, -2.0F, 56, 4, 4);
			ModelPart tipSkin = new ModelPart(this, -56, 144);
			tipSkin.addCuboid(-56.0F, 0.0F, 2.0F, 56, 0, 56);
			tip.add(tipSkin);
			wing.add(tip);
		}

		/** Membrane hangs down and back; the wing sweeps back and the tip folds as it flaps. */
		void pose(float t) {
			wing.posX = (float) Math.toRadians(-80.0F) - (float) Math.cos(t) * 0.2F;
			wing.posY = (float) Math.toRadians(20.0F) + (float) Math.sin(t) * 0.4F;
			wing.posZ = (float) Math.toRadians(20.0F);
			tip.posZ = -((float) Math.sin(t + 2.0F) + 0.5F) * 0.75F;
		}
	}
}
