package gg.optic.client.render;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 1.7 lowers the body into a crouch over a short ease. 1.8 snaps the pose
 * and the eye height on the same tick. This only changes how the pose is drawn.
 */
public final class OpticSneak {
	private static final Map<Entity, float[]> POSE = new WeakHashMap<Entity, float[]>();
	private static float frameDelta = 1.0F;

	private OpticSneak() {
	}

	public static boolean enabled() {
		OpticConfig config = OpticClient.get().config();
		return config.anim(config.animSneaking);
	}

	public static void tick(Entity entity) {
		if (entity == null) return;
		if (!enabled()) {
			POSE.remove(entity);
			return;
		}
		float[] pose = POSE.get(entity);
		if (pose == null) {
			float start = entity.isSneaking() ? 1.0F : 0.0F;
			pose = new float[] { start, start };
			POSE.put(entity, pose);
		}
		pose[1] = pose[0];
		float target = entity.isSneaking() ? 1.0F : 0.0F;
		pose[0] += (target - pose[0]) * 0.4F;
		if (Math.abs(target - pose[0]) < 0.02F) pose[0] = target;
	}

	public static void frame(float tickDelta) {
		frameDelta = tickDelta;
	}

	/** 0 standing, 1 fully crouched. -1 when the 1.7 option is off. */
	public static float visual(Entity entity) {
		if (!enabled() || entity == null) return -1.0F;
		float[] pose = POSE.get(entity);
		if (pose == null) return entity.isSneaking() ? 1.0F : 0.0F;
		float delta = frameDelta;
		if (delta < 0.0F) delta = 0.0F;
		if (delta > 1.0F) delta = 1.0F;
		return pose[1] + (pose[0] - pose[1]) * delta;
	}
}
