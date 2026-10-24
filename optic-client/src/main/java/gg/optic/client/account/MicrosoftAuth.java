package gg.optic.client.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Device-code Microsoft → Xbox → Minecraft login (same client id as the launcher). */
public final class MicrosoftAuth {
	private static final String MS_CLIENT_ID = "00000000402b5328";
	private static final String DEVICE = "https://login.live.com/oauth20_connect.srf";
	private static final String TOKEN = "https://login.live.com/oauth20_token.srf";
	private static final String XBL = "https://user.auth.xboxlive.com/user/authenticate";
	private static final String XSTS = "https://xsts.auth.xboxlive.com/xsts/authorize";
	private static final String MC_LOGIN = "https://api.minecraftservices.com/authentication/login_with_xbox";
	private static final String MC_PROFILE = "https://api.minecraftservices.com/minecraft/profile";

	public interface Listener {
		void onStatus(String message, String userCode);

		void onSuccess(SharedAccount account);

		void onError(String message);
	}

	private MicrosoftAuth() {
	}

	public static void startAsync(final Listener listener) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					MicrosoftAuth.runLogin(listener);
				} catch (Exception e) {
					listener.onError(e.getMessage() == null ? "Microsoft login failed." : e.getMessage());
				}
			}
		}, "optic-ms-auth");
		thread.setDaemon(true);
		thread.start();
	}

	private static void runLogin(Listener listener) throws Exception {
		listener.onStatus("Contacting Microsoft...", "");
		Map<String, String> deviceBody = new LinkedHashMap<String, String>();
		deviceBody.put("client_id", MS_CLIENT_ID);
		deviceBody.put("scope", "service::user.auth.xboxlive.com::MBI_SSL");
		deviceBody.put("response_type", "device_code");
		JsonObject device = postForm(DEVICE, deviceBody);
		if (device.has("error")) {
			throw new Exception(str(device, "error_description", str(device, "error", "device code failed")));
		}
		String deviceCode = str(device, "device_code", "");
		String userCode = str(device, "user_code", "");
		String verification = str(device, "verification_uri", str(device, "verification_url", "https://www.microsoft.com/link"));
		if (deviceCode.isEmpty() || userCode.isEmpty()) throw new Exception("Microsoft did not return a sign-in code.");

		try {
			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().browse(new URI(verification));
			}
		} catch (Exception ignored) {
		}

		listener.onStatus("Code copied. Paste it in the browser.", userCode);

		int interval = device.has("interval") ? device.get("interval").getAsInt() : 5;
		int expiresIn = device.has("expires_in") ? device.get("expires_in").getAsInt() : 900;
		long deadline = System.currentTimeMillis() + expiresIn * 1000L;
		String msToken = "";
		String refresh = "";
		int wait = Math.max(1, interval);

		while (System.currentTimeMillis() < deadline) {
			Thread.sleep(wait * 1000L);
			Map<String, String> tokenBody = new LinkedHashMap<String, String>();
			tokenBody.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
			tokenBody.put("client_id", MS_CLIENT_ID);
			tokenBody.put("device_code", deviceCode);
			JsonObject token = postForm(TOKEN, tokenBody);
			if (token.has("access_token")) {
				msToken = token.get("access_token").getAsString();
				refresh = str(token, "refresh_token", "");
				break;
			}
			String error = str(token, "error", "");
			if ("authorization_pending".equals(error)) continue;
			if ("slow_down".equals(error)) {
				wait += 5;
				continue;
			}
			if (!error.isEmpty()) {
				throw new Exception(str(token, "error_description", error));
			}
		}
		if (msToken.isEmpty()) throw new Exception("Device code expired before it was entered.");

		listener.onStatus("Signing into Xbox...", userCode);
		SharedAccount account = finishLogin(msToken, refresh);
		listener.onSuccess(account);
	}

	/** Exchange a stored Microsoft refresh token for a new Minecraft session. */
	public static SharedAccount refresh(SharedAccount account) throws Exception {
		if (account == null || !"microsoft".equalsIgnoreCase(account.kind)) return null;
		String stored = account.refreshToken;
		if (stored == null || stored.isEmpty() || stored.startsWith("local-offline")) return null;

		Map<String, String> body = new LinkedHashMap<String, String>();
		body.put("client_id", MS_CLIENT_ID);
		body.put("grant_type", "refresh_token");
		body.put("refresh_token", stored);
		body.put("scope", "service::user.auth.xboxlive.com::MBI_SSL");
		JsonObject token = postForm(TOKEN, body);
		if (token.has("error")) {
			throw new Exception(str(token, "error_description", str(token, "error", "Microsoft refresh failed.")));
		}
		String msToken = str(token, "access_token", "");
		if (msToken.isEmpty()) throw new Exception("Microsoft did not return a refreshed token.");
		String nextRefresh = str(token, "refresh_token", stored);
		return finishLogin(msToken, nextRefresh.isEmpty() ? stored : nextRefresh);
	}

	private static SharedAccount finishLogin(String msToken, String refresh) throws Exception {
		JsonObject xblReq = new JsonObject();
		JsonObject xblProps = new JsonObject();
		xblProps.addProperty("AuthMethod", "RPS");
		xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
		xblProps.addProperty("RpsTicket", "t=" + msToken);
		xblReq.add("Properties", xblProps);
		xblReq.addProperty("RelyingParty", "http://auth.xboxlive.com");
		xblReq.addProperty("TokenType", "JWT");
		JsonObject xbl = postJson(XBL, xblReq);
		String xblToken = str(xbl, "Token", "");
		String uhs = "";
		try {
			uhs = xbl.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
		} catch (Exception ignored) {
		}
		if (xblToken.isEmpty() || uhs.isEmpty()) throw new Exception("Xbox Live did not return a user hash.");

		JsonObject xstsReq = new JsonObject();
		JsonObject xstsProps = new JsonObject();
		xstsProps.addProperty("SandboxId", "RETAIL");
		com.google.gson.JsonArray userTokens = new com.google.gson.JsonArray();
		userTokens.add(new com.google.gson.JsonPrimitive(xblToken));
		xstsProps.add("UserTokens", userTokens);
		xstsReq.add("Properties", xstsProps);
		xstsReq.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
		xstsReq.addProperty("TokenType", "JWT");
		JsonObject xsts = postJson(XSTS, xstsReq);
		String xstsToken = str(xsts, "Token", "");
		if (xstsToken.isEmpty()) throw new Exception("XSTS authorization failed.");

		JsonObject mcReq = new JsonObject();
		mcReq.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
		JsonObject mc = postJson(MC_LOGIN, mcReq);
		String access = str(mc, "access_token", "");
		if (access.isEmpty()) throw new Exception("Minecraft services did not return a token.");

		JsonObject profile = getJson(MC_PROFILE, access);
		String name = str(profile, "name", "");
		String id = str(profile, "id", "");
		if (name.isEmpty() || id.isEmpty()) throw new Exception("This Microsoft account does not own Minecraft.");

		return new SharedAccount(id, name, id, "microsoft", access, refresh);
	}

	private static String str(JsonObject o, String key, String fallback) {
		if (o == null || !o.has(key) || o.get(key).isJsonNull()) return fallback;
		try {
			return o.get(key).getAsString();
		} catch (Exception e) {
			return fallback;
		}
	}

	private static JsonObject postForm(String url, Map<String, String> body) throws Exception {
		StringBuilder encoded = new StringBuilder();
		for (Map.Entry<String, String> e : body.entrySet()) {
			if (encoded.length() > 0) encoded.append('&');
			encoded.append(URLEncoder.encode(e.getKey(), "UTF-8"));
			encoded.append('=');
			encoded.append(URLEncoder.encode(e.getValue(), "UTF-8"));
		}
		byte[] bytes = encoded.toString().getBytes(StandardCharsets.UTF_8);
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setConnectTimeout(15000);
		conn.setReadTimeout(15000);
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("Accept", "application/json");
		OutputStream out = conn.getOutputStream();
		try {
			out.write(bytes);
		} finally {
			out.close();
		}
		return readJson(conn);
	}

	private static JsonObject postJson(String url, JsonObject body) throws Exception {
		byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setConnectTimeout(15000);
		conn.setReadTimeout(15000);
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "application/json");
		OutputStream out = conn.getOutputStream();
		try {
			out.write(bytes);
		} finally {
			out.close();
		}
		return readJson(conn);
	}

	private static JsonObject getJson(String url, String bearer) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setConnectTimeout(15000);
		conn.setReadTimeout(15000);
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Authorization", "Bearer " + bearer);
		conn.setRequestProperty("Accept", "application/json");
		return readJson(conn);
	}

	private static JsonObject readJson(HttpURLConnection conn) throws Exception {
		BufferedReader reader = new BufferedReader(
			new InputStreamReader(
				conn.getResponseCode() >= 400 && conn.getErrorStream() != null
					? conn.getErrorStream()
					: conn.getInputStream(),
				StandardCharsets.UTF_8
			)
		);
		StringBuilder sb = new StringBuilder();
		try {
			String line;
			while ((line = reader.readLine()) != null) sb.append(line);
		} finally {
			reader.close();
			conn.disconnect();
		}
		return new JsonParser().parse(sb.toString()).getAsJsonObject();
	}
}
