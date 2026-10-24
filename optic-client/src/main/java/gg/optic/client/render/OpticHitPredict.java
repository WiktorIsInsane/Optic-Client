package gg.optic.client.render;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.predicate.EntityPredicate;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Read-only copy of Minecraft 1.8 entity picking, shifted onto the interpolated
 * render position. It never changes the crosshair target, reach, or attack packets.
 * A swing is "confirmed" only after the server sends the hurt status for that entity.
 */
public final class OpticHitPredict {
	private static final int PREDICT_ONLY = 0xE2B15C;
	private static final int PREDICT_SYNC = 0x5CFF8A;
	private static final int CONFIRMED = 0xFFFFFF;
	private static final long CONFIRM_WINDOW_MS = 800L;
	private static final long CONFIRM_SHOW_MS = 450L;

	private static Entity predicted;
	private static Entity synced;
	private static int pendingId = -1;
	private static long pendingAt;
	private static int confirmedId = -1;
	private static long confirmedAt;

	private OpticHitPredict() {
	}

	public static void update(MinecraftClient client, float tickDelta) {
		predicted = null;
		synced = null;
		if (!enabled() || client == null || client.world == null || client.interactionManager == null) return;
		Entity camera = client.getCameraEntity();
		if (camera == null) return;
		synced = client.targetedEntity;
		predicted = trace(client, camera, tickDelta);
	}

	/** Called after vanilla has already chosen {@code targetedEntity} and sent the swing. */
	public static void onSwing(MinecraftClient client) {
		pendingId = -1;
		if (!enabled() || client == null) return;
		Entity sent = client.targetedEntity;
		if (sent == null || sent == client.player) return;
		pendingId = sent.getEntityId();
		pendingAt = System.currentTimeMillis();
	}

	/** Server entity-status 2 (hurt). Ignored unless it is the entity we just swung at. */
	public static void onServerHurt(Entity entity) {
		if (!enabled() || entity == null || pendingId < 0) return;
		if (entity.getEntityId() != pendingId) return;
		if (System.currentTimeMillis() - pendingAt > CONFIRM_WINDOW_MS) return;
		confirmedId = entity.getEntityId();
		confirmedAt = System.currentTimeMillis();
		pendingId = -1;
	}

	public static String hudLabel() {
		if (!enabled()) return null;
		if (confirming()) return "CONFIRM";
		if (predicted != null) return "PREDICT";
		return null;
	}

	public static int hudColor() {
		if (confirming()) return 0xFF000000 | CONFIRMED;
		if (predicted != null && synced != null && predicted.getEntityId() == synced.getEntityId()) {
			return 0xFF000000 | PREDICT_SYNC;
		}
		return 0xFF000000 | PREDICT_ONLY;
	}

	public static void draw(Entity camera, float tickDelta) {
		if (!enabled() || camera == null) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return;

		double cx = camera.prevTickX + (camera.x - camera.prevTickX) * tickDelta;
		double cy = camera.prevTickY + (camera.y - camera.prevTickY) * tickDelta;
		double cz = camera.prevTickZ + (camera.z - camera.prevTickZ) * tickDelta;

		GlStateManager.depthMask(false);
		GlStateManager.disableTexture();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GL11.glLineWidth(2.0F);

		Entity shown = predicted;
		boolean confirm = confirming();
		if (confirm) {
			Entity confirmed = client.world.getEntityById(confirmedId);
			if (confirmed != null) shown = confirmed;
		}
		if (shown != null) {
			Box visual = interpolated(shown, tickDelta).offset(-cx, -cy, -cz);
			Box tick = shown.getBoundingBox().offset(-cx, -cy, -cz);
			if (separated(visual, tick)) {
				paint(tick, 0xFFFFFF, 70);
			}
			int color = confirm ? CONFIRMED : (same(shown, synced) ? PREDICT_SYNC : PREDICT_ONLY);
			paint(visual, color, 255);
		}
		if (!confirm && synced != null && !same(synced, predicted)) {
			paint(synced.getBoundingBox().offset(-cx, -cy, -cz), PREDICT_SYNC, 140);
		}

		GL11.glLineWidth(1.0F);
		GlStateManager.enableTexture();
		GlStateManager.enableLighting();
		GlStateManager.enableCull();
		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/**
	 * Same pick as {@code GameRenderer.updateTargetedEntity}, except each candidate
	 * box is moved to {@code prev + (pos - prev) * tickDelta} before the ray test.
	 * Reach, the 0.1 targeting margin, and the 3-block survival cap are unchanged.
	 */
	private static Entity trace(MinecraftClient client, Entity camera, float tickDelta) {
		double reach = client.interactionManager.getReachDistance();
		BlockHitResult block = camera.rayTrace(reach, tickDelta);
		double blockDistance = reach;
		Vec3d eyes = camera.getCameraPosVec(tickDelta);
		boolean capSurvival = false;
		if (client.interactionManager.hasExtendedReach()) {
			reach = 6.0;
			blockDistance = 6.0;
		} else if (reach > 3.0) {
			capSurvival = true;
		}
		if (block != null) {
			blockDistance = block.pos.distanceTo(eyes);
		}

		Vec3d look = camera.getRotationVector(tickDelta);
		Vec3d end = eyes.add(look.x * reach, look.y * reach, look.z * reach);
		Box search = camera.getBoundingBox().stretch(look.x * reach, look.y * reach, look.z * reach).expand(1.0, 1.0, 1.0).expand(1.0, 1.0, 1.0);
		List<Entity> list = client.world.getEntitiesIn(camera, search, Predicates.and(EntityPredicate.EXCEPT_SPECTATOR, new Predicate<Entity>() {
			@Override
			public boolean apply(Entity entity) {
				return entity.collides();
			}
		}));

		Entity hit = null;
		Vec3d hitPos = null;
		double closest = blockDistance;
		for (int i = 0; i < list.size(); i++) {
			Entity other = list.get(i);
			float margin = other.getTargetingMargin();
			Box box = interpolated(other, tickDelta).expand(margin, margin, margin);
			BlockHitResult intercept = box.method_585(eyes, end);
			if (box.contains(eyes)) {
				if (closest >= 0.0) {
					hit = other;
					hitPos = intercept == null ? eyes : intercept.pos;
					closest = 0.0;
				}
			} else if (intercept != null) {
				double distance = eyes.distanceTo(intercept.pos);
				if (distance < closest || closest == 0.0) {
					if (other == camera.vehicle) {
						if (closest == 0.0) {
							hit = other;
							hitPos = intercept.pos;
						}
					} else {
						hit = other;
						hitPos = intercept.pos;
						closest = distance;
					}
				}
			}
		}

		if (hit != null && capSurvival && hitPos != null && eyes.distanceTo(hitPos) > 3.0) {
			hit = null;
		}
		if (hit != null && !(closest < blockDistance || block == null)) {
			hit = null;
		}
		if (hit instanceof LivingEntity || hit instanceof ItemFrameEntity) return hit;
		return null;
	}

	private static Box interpolated(Entity entity, float tickDelta) {
		double x = entity.prevTickX + (entity.x - entity.prevTickX) * tickDelta;
		double y = entity.prevTickY + (entity.y - entity.prevTickY) * tickDelta;
		double z = entity.prevTickZ + (entity.z - entity.prevTickZ) * tickDelta;
		Box box = entity.getBoundingBox();
		return box.offset(x - entity.x, y - entity.y, z - entity.z);
	}

	private static boolean separated(Box a, Box b) {
		return Math.abs(a.minX - b.minX) > 0.01
			|| Math.abs(a.minY - b.minY) > 0.01
			|| Math.abs(a.minZ - b.minZ) > 0.01;
	}

	private static boolean same(Entity a, Entity b) {
		return a != null && b != null && a.getEntityId() == b.getEntityId();
	}

	private static boolean confirming() {
		return confirmedId >= 0 && System.currentTimeMillis() - confirmedAt <= CONFIRM_SHOW_MS;
	}

	private static boolean enabled() {
		return OpticClient.get().config().hitPredict;
	}

	private static void paint(Box box, int rgb, int alpha) {
		WorldRenderer.drawBox(box, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha);
	}
}
