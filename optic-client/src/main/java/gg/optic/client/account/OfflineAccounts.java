package gg.optic.client.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

public final class OfflineAccounts {
	private OfflineAccounts() {
	}

	public static SharedAccount create(String username) {
		String name = username == null ? "" : username.trim();
		if (!name.matches("^[A-Za-z0-9_]{3,16}$")) {
			throw new IllegalArgumentException("Username must be 3-16 chars: letters, numbers, _");
		}
		String uuid = offlineUuid(name);
		String token = offlineToken(name);
		return new SharedAccount(uuid.replace("-", ""), name, uuid, "offline", token, "local-offline-" + uuid);
	}

	private static String offlineUuid(String username) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
			hash[6] = (byte) ((hash[6] & 0x0f) | 0x30);
			hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
			StringBuilder sb = new StringBuilder();
			for (byte b : hash) sb.append(String.format("%02x", b));
			String hex = sb.toString();
			return hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16)
				+ "-" + hex.substring(16, 20) + "-" + hex.substring(20);
		} catch (Exception e) {
			return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString();
		}
	}

	private static String offlineToken(String username) {
		// Lightweight placeholder token (offline servers ignore it).
		return "offline-" + username;
	}
}
