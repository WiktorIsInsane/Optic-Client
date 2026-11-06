package gg.optic.client.render;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Hit Predict (visual only): the moment you hit someone, they flash red on your screen
 * instead of a round trip later when the server says so. Nothing extra is sent and nothing
 * about the hit itself changes; it only stops ping from making hits look late.
 *
 * Only predicted when the hit is very likely to count: a living target within 3 blocks that
 * is not still in its hurt animation (the server ignores those hits anyway).
 * When the server's real hurt arrives, the flash already playing is kept instead of restarted.
 */
public final class OpticHitPredict {
	private static final long CONFIRM_WINDOW_MS = 800L;
	private static final Map<Entity, Long> PREDICTED = new WeakHashMap<Entity, Long>();

	private OpticHitPredict() {
	}

	private static boolean enabled() {
		OpticClient optic = OpticClient.get();
		OpticConfig c = optic == null ? null : optic.config();
		return c != null && c.hitPredict;
	}

	/** After you attack an entity. */
	public static void onAttack(PlayerEntity player, Entity target) {
		if (!enabled() || player == null || !(target instanceof LivingEntity)) return;
		LivingEntity living = (LivingEntity) target;
		if (living.hurtTime > 0 || living.getHealth() <= 0.0F) return;
		if (player.distanceTo(target) > 3.0F) return;
		living.maxHurtTime = 10;
		living.hurtTime = 10;
		PREDICTED.put(target, Long.valueOf(System.currentTimeMillis()));
	}

	/**
	 * The server's hurt for this entity arrived. @return true to keep the flash that is
	 * already playing (it was predicted a moment ago) rather than restart it.
	 */
	public static boolean keepPredicted(LivingEntity entity, int hurtTimeBefore) {
		Long at = PREDICTED.remove(entity);
		return at != null && hurtTimeBefore > 0 && System.currentTimeMillis() - at.longValue() < CONFIRM_WINDOW_MS;
	}
}
