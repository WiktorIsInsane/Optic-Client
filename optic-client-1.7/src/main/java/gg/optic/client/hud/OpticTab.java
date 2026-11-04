package gg.optic.client.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticFreeLook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.class_482;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 1.8.9-style player list for 1.7.10. Replaces 1.7's tab, which draws a fixed grid sized to the
 * server's max player count, with 1.8.9's layout: only the listed players, sorted by team and
 * name, at most 20 rows per column, with heads and ping bars. Used whether or not the Tab module
 * is on; the module only adds its own styling options and keybind.
 *
 * Tab entries on 1.7 are only a name (field_1679) and a ping (field_1680), with no profile,
 * display name, header or footer.
 */
public final class OpticTab {
	private static final int MAX_ENTRIES = 80;
	private static OpticConfig vanilla;
	private static boolean latched;
	private static boolean wasDown;

	private OpticTab() {
	}

	/** Module settings when Tab is on, otherwise stock defaults (plain 1.8.9 look). */
	private static OpticConfig style() {
		OpticConfig c = OpticClient.get().config();
		if (c.tab) return c;
		if (vanilla == null) vanilla = new OpticConfig();
		return vanilla;
	}

	public static void tick(MinecraftClient client) {
		OpticConfig c = OpticClient.get().config();
		if (!c.tab || client == null || client.currentScreen != null) {
			if (client != null && client.currentScreen != null) {
				latched = false;
				wasDown = false;
			}
			return;
		}
		boolean down = OpticFreeLook.keyDown(c.tabKeyCode);
		if (c.tabToggle) {
			if (down && !wasDown) latched = !latched;
		}
		wasDown = down;
	}

	public static boolean visible(MinecraftClient client) {
		OpticConfig c = OpticClient.get().config();
		if (client.currentScreen != null || client.getNetworkHandler() == null || client.field_3805 == null) return false;
		if (!c.tab) {
			if (client.options == null || client.options.playerListKey == null || !client.options.playerListKey.isPressed()) return false;
			// Same rule as vanilla: in singleplayer only once someone else has joined.
			List<?> raw = client.getNetworkHandler().field_7911;
			return !client.isIntegratedServerRunning() || (raw != null && raw.size() > 1);
		}
		if (c.tabToggle) return latched;
		if (c.tabKeyCode == Keyboard.KEY_NONE) return false;
		return OpticFreeLook.keyDown(c.tabKeyCode);
	}

	public static List<class_482> entries(final MinecraftClient client) {
		OpticConfig c = style();
		List<?> raw = client.getNetworkHandler().field_7911;
		List<class_482> list = new ArrayList<class_482>();
		String self = selfName(client);
		if (raw != null) {
			for (Object item : raw) {
				if (!(item instanceof class_482)) continue;
				class_482 entry = (class_482) item;
				if (entry.field_1679 == null) continue;
				if (c.tabHideNpc && entry.field_1680 <= 0 && !entry.field_1679.equals(self)) continue;
				list.add(entry);
			}
		}
		Collections.sort(list, new Comparator<class_482>() {
			@Override
			public int compare(class_482 a, class_482 b) {
				Team ta = team(client, a);
				Team tb = team(client, b);
				String na = ta == null ? "" : ta.getName();
				String nb = tb == null ? "" : tb.getName();
				int team = na.compareTo(nb);
				if (team != 0) return team;
				return a.field_1679.compareToIgnoreCase(b.field_1679);
			}
		});
		if (c.tabMoveSelfTop && self != null) {
			for (int i = 0; i < list.size(); i++) {
				if (self.equals(list.get(i).field_1679)) {
					list.add(0, list.remove(i));
					break;
				}
			}
		}
		if (list.size() > MAX_ENTRIES) list = new ArrayList<class_482>(list.subList(0, MAX_ENTRIES));
		return list;
	}

	private static String selfName(MinecraftClient client) {
		return client.field_3805 == null ? null : client.field_3805.getGameProfile().getName();
	}

	private static Team team(MinecraftClient client, class_482 entry) {
		Scoreboard scoreboard = client.world == null ? null : client.world.getScoreboard();
		return scoreboard == null ? null : scoreboard.getPlayerTeam(entry.field_1679);
	}

	public static String name(class_482 entry) {
		Team team = team(MinecraftClient.getInstance(), entry);
		if (team != null) return Team.decorateName(team, entry.field_1679);
		return entry.field_1679;
	}

	public static int nameColor(OpticConfig c, MinecraftClient client, class_482 entry) {
		if (c.tabHighlightSelf && entry.field_1679.equals(selfName(client))) {
			return c.tabSelfColor;
		}
		return 0xFFFFFF;
	}

	public static String pingLabel(OpticConfig c, int ping) {
		if (c.tabHidePing) return "";
		if (c.tabHidePingOver500 && ping > 500) return "";
		if (!c.tabPingNumbers) return null;
		return Integer.toString(Math.max(0, ping));
	}

	public static int pingColor(OpticConfig c, int ping) {
		if (!c.tabDynamicPing) return c.tabPingColor;
		return OpticPing.colorFor(c, ping);
	}

	/** 1.8.9 icon row: 0 = full bars ... 4 = one bar, 5 = no connection. */
	public static int pingIcon(int ping) {
		if (ping < 0) return 5;
		if (ping < 150) return 0;
		if (ping < 300) return 1;
		if (ping < 600) return 2;
		if (ping < 1000) return 3;
		return 4;
	}

	public static int score(Scoreboard scoreboard, ScoreboardObjective objective, class_482 entry) {
		if (scoreboard == null || objective == null) return Integer.MIN_VALUE;
		return scoreboard.getPlayerScore(entry.field_1679, objective).getScore();
	}

	/** 1.7 tab entries carry no skin; use the player's skin when they are loaded nearby. */
	public static Identifier skin(MinecraftClient client, class_482 entry) {
		PlayerEntity player = client.world == null ? null : client.world.getPlayerByName(entry.field_1679);
		if (player instanceof AbstractClientPlayerEntity) {
			Identifier skin = ((AbstractClientPlayerEntity) player).getSkinId();
			if (skin != null) return skin;
		}
		return AbstractClientPlayerEntity.STEVE_TEXTURE;
	}

	/** Layout follows 1.8.9's PlayerListHud.render. */
	public static void render(
		MinecraftClient client,
		int screenWidth,
		Scoreboard scoreboard,
		ScoreboardObjective objective
	) {
		OpticConfig c = style();
		List<class_482> list = entries(client);
		if (list.isEmpty()) return;

		int count = list.size();
		int rows = count;
		int cols = 1;
		while (rows > 20) {
			cols++;
			rows = (count + cols - 1) / cols;
		}

		boolean heads = c.tabHeads;
		boolean showPing = !c.tabHidePing;
		boolean numbers = c.tabPingNumbers;
		int nameW = 0;
		int scoreW = 0;
		for (int i = 0; i < count; i++) {
			class_482 entry = list.get(i);
			nameW = Math.max(nameW, client.textRenderer.getStringWidth(name(entry)));
			if (objective != null) {
				int value = score(scoreboard, objective, entry);
				if (value != Integer.MIN_VALUE) {
					scoreW = Math.max(scoreW, client.textRenderer.getStringWidth(" " + value));
				}
			}
		}
		int pingW = showPing ? (numbers ? 24 : 13) : 2;
		int colW = Math.min(cols * ((heads ? 9 : 0) + nameW + scoreW + pingW), screenWidth - 50) / cols;
		int left = screenWidth / 2 - (colW * cols + (cols - 1) * 5) / 2;
		int top = 10;
		int gridW = colW * cols + (cols - 1) * 5;

		int bg = c.tab ? OpticHud.withAlpha(c.tabBackgroundColor, 0x80) : 0x80000000;
		DrawableHelper.fill(screenWidth / 2 - gridW / 2 - 1, top - 1, screenWidth / 2 + gridW / 2 + 1, top + rows * 9, bg);

		for (int i = 0; i < count; i++) {
			int col = i / rows;
			int row = i % rows;
			int x = left + col * colW + col * 5;
			int y = top + row * 9;
			DrawableHelper.fill(x, y, x + colW, y + 8, 0x20FFFFFF);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.enableAlphaTest();
			GlStateManager.enableBlend();
			GlStateManager.blendFuncSeparate(770, 771, 1, 0);

			class_482 entry = list.get(i);
			int textX = x;
			if (heads) {
				drawHead(client, entry, x, y);
				textX += 9;
			}
			String label = name(entry);
			int color = 0xFF000000 | (nameColor(c, client, entry) & 0xFFFFFF);
			client.textRenderer.draw(label, textX, y, color, c.tabNameShadow);

			if (objective != null) {
				int value = score(scoreboard, objective, entry);
				if (value != Integer.MIN_VALUE) {
					String scoreText = Integer.toString(value);
					int right = x + colW - pingW;
					int sx = right - client.textRenderer.getStringWidth(scoreText);
					if (sx > textX + client.textRenderer.getStringWidth(label)) {
						client.textRenderer.draw(scoreText, sx, y, 0xFFFFFF55, c.tabNameShadow);
					}
				}
			}
			if (showPing) drawPing(client, c, entry, x, colW, y);
		}
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void drawHead(MinecraftClient client, class_482 entry, int x, int y) {
		client.getTextureManager().bindTexture(skin(client, entry));
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableBlend();
		// 1.7 skins are 64x32.
		DrawableHelper.drawTexture(x, y, 8.0F, 8.0F, 8, 8, 64.0F, 32.0F);
		DrawableHelper.drawTexture(x, y, 40.0F, 8.0F, 8, 8, 64.0F, 32.0F);
	}

	private static void drawPing(MinecraftClient client, OpticConfig c, class_482 entry, int x, int colW, int y) {
		int ping = entry.field_1680;
		String label = pingLabel(c, ping);
		if (label == null) {
			client.getTextureManager().bindTexture(DrawableHelper.GUI_ICONS_TEXTURE);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			int index = pingIcon(ping);
			DrawableHelper.drawTexture(x + colW - 11, y, 0.0F, 176.0F + index * 8.0F, 10, 8, 256.0F, 256.0F);
			return;
		}
		if (label.length() == 0) return;
		int color = 0xFF000000 | (pingColor(c, ping) & 0xFFFFFF);
		int lx = x + colW - 1 - client.textRenderer.getStringWidth(label);
		client.textRenderer.draw(label, lx, y, color, c.tabNameShadow);
	}
}
