package gg.optic.client.account;

public final class SharedAccount {
	public final String id;
	public final String username;
	public final String uuid;
	public final String kind;
	public final String accessToken;
	public final String refreshToken;

	public SharedAccount(String id, String username, String uuid, String kind) {
		this(id, username, uuid, kind, "", "");
	}

	public SharedAccount(
		String id,
		String username,
		String uuid,
		String kind,
		String accessToken,
		String refreshToken
	) {
		this.id = id;
		this.username = username;
		this.uuid = uuid;
		this.kind = kind == null || kind.isEmpty() ? "offline" : kind;
		this.accessToken = accessToken == null ? "" : accessToken;
		this.refreshToken = refreshToken == null ? "" : refreshToken;
	}

	public String sessionToken() {
		if (accessToken != null && !accessToken.isEmpty()) return accessToken;
		return "0";
	}
}
