package gg.optic.client.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared account roster at {@code %APPDATA%\.optic\shared\accounts.json}
 * (legacy {@code .lunar-remake} supported). Includes tokens so the game can
 * hot-swap sessions without relaunching.
 */
public final class SharedAccounts {
	private SharedAccounts() {
	}

	public static File sharedFile() {
		String appData = System.getenv("APPDATA");
		if (appData == null || appData.isEmpty()) {
			appData = System.getProperty("user.home", ".");
		}
		File optic = new File(appData, ".optic/shared/accounts.json");
		File legacy = new File(appData, ".lunar-remake/shared/accounts.json");
		if (optic.isFile()) return optic;
		if (legacy.isFile()) return legacy;
		return optic;
	}

	public static Snapshot load() {
		File file = sharedFile();
		if (!file.isFile()) return Snapshot.empty();
		try {
			String json = readAll(file);
			JsonObject root = new JsonParser().parse(json).getAsJsonObject();
			String active = root.has("activeAccountId") ? root.get("activeAccountId").getAsString() : "";
			List<SharedAccount> accounts = new ArrayList<SharedAccount>();
			if (root.has("accounts") && root.get("accounts").isJsonArray()) {
				JsonArray arr = root.getAsJsonArray("accounts");
				for (JsonElement el : arr) {
					if (!el.isJsonObject()) continue;
					JsonObject o = el.getAsJsonObject();
					String id = str(o, "id");
					String username = str(o, "username");
					String uuid = str(o, "uuid");
					if (id.isEmpty() || username.isEmpty()) continue;
					if (uuid.isEmpty()) uuid = id;
					accounts.add(new SharedAccount(
						id,
						username,
						uuid,
						str(o, "kind"),
						str(o, "accessToken"),
						str(o, "refreshToken")
					));
				}
			}
			if (active.isEmpty() && !accounts.isEmpty()) active = accounts.get(0).id;
			return new Snapshot(active, accounts);
		} catch (Exception e) {
			System.err.println("[Optic] Shared accounts read failed: " + e.getMessage());
			return Snapshot.empty();
		}
	}

	public static void save(Snapshot snap) {
		JsonObject root = new JsonObject();
		root.addProperty("version", 1);
		root.addProperty("activeAccountId", snap.activeAccountId == null ? "" : snap.activeAccountId);
		JsonArray arr = new JsonArray();
		for (SharedAccount a : snap.accounts) {
			JsonObject o = new JsonObject();
			o.addProperty("id", a.id);
			o.addProperty("username", a.username);
			o.addProperty("uuid", a.uuid);
			o.addProperty("kind", a.kind);
			if (a.accessToken != null && !a.accessToken.isEmpty()) {
				o.addProperty("accessToken", a.accessToken);
			}
			if (a.refreshToken != null && !a.refreshToken.isEmpty()) {
				o.addProperty("refreshToken", a.refreshToken);
			}
			arr.add(o);
		}
		root.add("accounts", arr);
		root.addProperty("updatedAt", System.currentTimeMillis());

		File file = sharedFile();
		file.getParentFile().mkdirs();
		try {
			BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)
			);
			try {
				writer.write(root.toString());
			} finally {
				writer.close();
			}
		} catch (Exception e) {
			System.err.println("[Optic] Shared accounts write failed: " + e.getMessage());
		}
	}

	public static void setActive(String accountId) {
		Snapshot snap = load();
		boolean found = false;
		for (SharedAccount account : snap.accounts) {
			if (account.id.equals(accountId)) {
				found = true;
				break;
			}
		}
		if (!found) return;
		save(new Snapshot(accountId, snap.accounts));
	}

	public static void remove(String accountId) {
		if (accountId == null || accountId.isEmpty()) return;
		Snapshot snap = load();
		List<SharedAccount> next = new ArrayList<SharedAccount>();
		for (SharedAccount existing : snap.accounts) {
			if (!existing.id.equals(accountId)) next.add(existing);
		}
		String active = snap.activeAccountId;
		if (accountId.equals(active)) {
			active = next.isEmpty() ? "" : next.get(0).id;
		}
		save(new Snapshot(active, next));
	}

	public static void upsert(SharedAccount account, boolean makeActive) {
		Snapshot snap = load();
		List<SharedAccount> next = new ArrayList<SharedAccount>();
		for (SharedAccount existing : snap.accounts) {
			if (!existing.id.equals(account.id)) next.add(existing);
		}
		next.add(account);
		String active = makeActive ? account.id : snap.activeAccountId;
		if (active == null || active.isEmpty()) active = account.id;
		save(new Snapshot(active, next));
	}

	private static String str(JsonObject o, String key) {
		if (!o.has(key) || o.get(key).isJsonNull()) return "";
		try {
			return o.get(key).getAsString();
		} catch (Exception e) {
			return "";
		}
	}

	private static String readAll(File file) throws Exception {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = new BufferedReader(
			new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
		);
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
		} finally {
			reader.close();
		}
		return sb.toString();
	}

	public static final class Snapshot {
		public final String activeAccountId;
		public final List<SharedAccount> accounts;

		public Snapshot(String activeAccountId, List<SharedAccount> accounts) {
			this.activeAccountId = activeAccountId;
			this.accounts = accounts;
		}

		public static Snapshot empty() {
			return new Snapshot("", new ArrayList<SharedAccount>());
		}

		public SharedAccount active() {
			for (SharedAccount account : accounts) {
				if (account.id.equals(activeAccountId)) return account;
			}
			return accounts.isEmpty() ? null : accounts.get(0);
		}

		public SharedAccount byId(String id) {
			for (SharedAccount account : accounts) {
				if (account.id.equals(id)) return account;
			}
			return null;
		}
	}
}
