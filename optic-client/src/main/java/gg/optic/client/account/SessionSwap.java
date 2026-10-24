package gg.optic.client.account;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Session;

/** Hot-swaps the client Session without restarting the game. */
public final class SessionSwap {
	private SessionSwap() {
	}

	public static void apply(MinecraftClient client, SharedAccount account) {
		if (client == null || account == null) return;
		String type = "microsoft".equalsIgnoreCase(account.kind) ? "mojang" : "legacy";
		Session session = new Session(account.username, normalizeUuid(account.uuid), account.sessionToken(), type);
		((SessionHolder) (Object) client).optic$setSession(session);
		SharedAccounts.setActive(account.id);
		System.out.println("[Optic] Session swapped -> " + account.username);
	}

	public static void applyNew(MinecraftClient client, SharedAccount account) {
		SharedAccounts.upsert(account, true);
		apply(client, account);
	}

	private static String normalizeUuid(String uuid) {
		if (uuid == null) return "";
		String raw = uuid.replace("-", "");
		if (raw.length() != 32) return uuid;
		return raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-" + raw.substring(12, 16)
			+ "-" + raw.substring(16, 20) + "-" + raw.substring(20);
	}

	public interface SessionHolder {
		void optic$setSession(Session session);
	}
}
