package gg.optic.client.render;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.class_482;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nick Hider: swaps player names for nicknames in every piece of text the game draws (chat,
 * tab, nametags, scoreboard...), by rewriting strings on their way into the font renderer.
 * Your name becomes "Own Name"; others become Prefix-1, Prefix-2... (or Prefix + Suffix).
 * Can also show Steve instead of real skins, and hide Hypixel's lobby-id messages.
 */
public final class OpticNickHider {
	private static final Pattern LOBBY_SENDING = Pattern.compile("Sending to server ([a-zA-Z0-9])+(\\.\\.\\.|!)");
	private static final Pattern LOBBY_HUB = Pattern.compile("Request join for Hub (#[0-9]+ \\()?(mini|mega)[0-9]+[A-Z]\\)?(\\.\\.\\.|!)");
	private static final Pattern LOBBY_YOU = Pattern.compile("Sending you to (mini|mega)[0-9]+[A-Z](\\.\\.\\.|!)");
	private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z0-9_]{3,16}");

	/** Lowercase real name -> what to show. */
	private static Map<String, String> replacements = Collections.emptyMap();
	private static Pattern matcher;
	/** Others get stable numbers for as long as you stay in the same world. */
	private static final Map<String, Integer> NUMBERS = new HashMap<String, Integer>();
	private static ClientWorld numberedWorld;
	private static String settingsKey = "";
	private static long lastRefresh;
	private static final Map<String, String> CACHE = new LinkedHashMap<String, String>(256, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return size() > 4000;
		}
	};

	private OpticNickHider() {
	}

	private static OpticConfig config() {
		OpticClient optic = OpticClient.get();
		return optic == null ? null : optic.config();
	}

	public static boolean active() {
		OpticConfig c = config();
		return c != null && c.nickHider;
	}

	/** Text with hidden names swapped. Called for every string the font renderer draws or measures. */
	public static String transform(String text) {
		if (text == null || text.isEmpty() || !active()) return text;
		refresh();
		if (matcher == null) return text;
		String cached = CACHE.get(text);
		if (cached != null) return cached;
		String out = replace(text);
		CACHE.put(text, out);
		return out;
	}

	/** Whether this player's skin should be drawn as Steve instead. */
	public static boolean hideSkin(AbstractClientPlayerEntity player) {
		OpticConfig c = config();
		if (c == null || !c.nickHider || player == null) return false;
		boolean self = player == MinecraftClient.getInstance().field_3805;
		return self ? c.nickHideOwnSkin : c.nickHideOthersSkin;
	}

	/** Hypixel lobby-id chat lines to drop. */
	public static boolean hideChatLine(String raw) {
		OpticConfig c = config();
		if (c == null || !c.nickHider || !c.nickHideLobbyId || raw == null) return false;
		String plain = raw.replaceAll("§.", "").trim();
		return LOBBY_SENDING.matcher(plain).matches() || LOBBY_HUB.matcher(plain).matches() || LOBBY_YOU.matcher(plain).matches();
	}

	/** Rebuilds the name list at most twice a second, or right away when a setting changed. */
	private static void refresh() {
		OpticConfig c = config();
		String key = c.nickHideOwnName + "|" + c.nickOwnName + "|" + c.nickHideOthers + "|" + c.nickPrefix + "|" + c.nickCustomSuffix + "|" + c.nickSuffix;
		long now = System.currentTimeMillis();
		if (key.equals(settingsKey) && now - lastRefresh < 500L) return;
		lastRefresh = now;

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world != numberedWorld) {
			numberedWorld = client.world;
			NUMBERS.clear();
		}
		String self = client.getSession() == null ? null : client.getSession().getUsername();
		Map<String, String> next = new HashMap<String, String>();
		if (c.nickHideOwnName && self != null && !self.isEmpty()) {
			next.put(self.toLowerCase(), c.nickOwnName.isEmpty() ? "You" : c.nickOwnName);
		}
		if (c.nickHideOthers) {
			for (String name : otherNames(client)) {
				if (self != null && name.equalsIgnoreCase(self)) continue;
				String lower = name.toLowerCase();
				Integer number = NUMBERS.get(lower);
				if (number == null) {
					number = Integer.valueOf(NUMBERS.size() + 1);
					NUMBERS.put(lower, number);
				}
				String prefix = c.nickPrefix.isEmpty() ? "Player" : c.nickPrefix;
				next.put(lower, c.nickCustomSuffix ? prefix + c.nickSuffix : prefix + "-" + number);
			}
		}
		if (key.equals(settingsKey) && next.equals(replacements)) return;
		settingsKey = key;
		replacements = next;
		CACHE.clear();
		if (next.isEmpty()) {
			matcher = null;
			return;
		}
		// Longest names first so "Steve12" wins over "Steve".
		List<String> names = new ArrayList<String>(next.keySet());
		Collections.sort(names, new Comparator<String>() {
			@Override
			public int compare(String a, String b) {
				return b.length() - a.length();
			}
		});
		StringBuilder regex = new StringBuilder();
		for (String name : names) {
			if (regex.length() > 0) regex.append('|');
			regex.append(Pattern.quote(name));
		}
		matcher = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
	}

	/** Names from the tab list and from players loaded in the world. */
	private static List<String> otherNames(MinecraftClient client) {
		List<String> names = new ArrayList<String>();
		try {
			if (client.getNetworkHandler() != null && client.getNetworkHandler().field_7911 != null) {
				for (Object item : client.getNetworkHandler().field_7911) {
					if (item instanceof class_482) {
						String name = ((class_482) item).field_1679;
						if (name != null) names.add(name.replaceAll("§.", ""));
					}
				}
			}
			if (client.world != null) {
				for (Object item : client.world.playerEntities) {
					if (item instanceof PlayerEntity) names.add(((PlayerEntity) item).getGameProfile().getName());
				}
			}
		} catch (Exception ignored) {
			// Lists can change under us mid-tick; the next refresh picks it up.
		}
		List<String> valid = new ArrayList<String>();
		for (String name : names) {
			if (name != null && VALID_NAME.matcher(name).matches()) valid.add(name);
		}
		return valid;
	}

	/**
	 * Whole-name matches only, so "Steve" inside "Stevenson" stays. A colour code right before
	 * the name ("§aSteve") still counts as a boundary.
	 */
	private static String replace(String text) {
		Matcher m = matcher.matcher(text);
		StringBuilder out = null;
		int last = 0;
		while (m.find()) {
			int start = m.start();
			int end = m.end();
			if (!boundaryBefore(text, start) || (end < text.length() && isNameChar(text.charAt(end)))) continue;
			String to = replacements.get(m.group().toLowerCase());
			if (to == null) continue;
			if (out == null) out = new StringBuilder(text.length() + 8);
			out.append(text, last, start).append(to);
			last = end;
		}
		if (out == null) return text;
		out.append(text, last, text.length());
		return out.toString();
	}

	private static boolean boundaryBefore(String text, int index) {
		if (index == 0) return true;
		if (!isNameChar(text.charAt(index - 1))) return true;
		return index >= 2 && text.charAt(index - 2) == '§';
	}

	private static boolean isNameChar(char ch) {
		return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '_';
	}
}
