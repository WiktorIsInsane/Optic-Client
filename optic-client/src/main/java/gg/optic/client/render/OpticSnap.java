package gg.optic.client.render;

import gg.optic.client.OpticClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.OtherClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * After a hit, keep the other player's position blend at the normal length.
 * Their position is never jumped, and no attack data is added.
 */
public final class OpticSnap {
	private static final long WINDOW_MS = 800L;
	private static final Map<Entity, Long> HITS = new IdentityHashMap<Entity, Long>();

	private OpticSnap() {
	}

	public static void onHit(PlayerEntity attacker, Entity target) {
		if (!OpticClient.get().config().opticSnap) return;
		if (!(target instanceof OtherClientPlayerEntity) || target == attacker) return;
		HITS.put(target, Long.valueOf(System.currentTimeMillis() + WINDOW_MS));
	}

	/** True while a recent hit should keep the vanilla slide instead of a one-step jump. */
	public static boolean smooth(Entity entity) {
		Long until = HITS.get(entity);
		if (until == null) return false;
		if (!OpticClient.get().config().opticSnap || System.currentTimeMillis() > until.longValue()) {
			HITS.remove(entity);
			return false;
		}
		return true;
	}
}
