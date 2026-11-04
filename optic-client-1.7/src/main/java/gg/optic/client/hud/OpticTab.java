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
 * Player-list layout for 1.7.10. Tab entries there are only a name (field_1679) and a
 * ping (field_1680), with no profile, display name, header or footer.
 */
public final class OpticTab {
	private static boolean latched;
	private static boolean wasDown;

	private OpticTab() {
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
		if (!c.tab || client.currentScreen != null || client.getNetworkHandler() == null) return false;
		if (c.tabToggle) return latched;
		if (c.tabKeyCode == Keyboard.KEY_NONE) return false;
		return OpticFreeLook.keyDown(c.tabKeyCode);
	}

	public static List<class_482> entries(final MinecraftClient client) {
		OpticConfig c = OpticClient.get().config();
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
				int team = na.compareToIgnoreCase(nb);
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

	public static int bars(int ping) {
		if (ping < 0) return 0;
		if (ping < 150) return 5;
		if (ping < 300) return 4;
		if (ping < 600) return 3;
		if (ping < 1000) return 2;
		return 1;
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

	public static void render(
		MinecraftClient client,
		int screenWidth,
		Scoreboard scoreboard,
		ScoreboardObjective objective
	) {
		OpticConfig c = OpticClient.get().config();
		List<class_482> list = entries(client);
		// 1.7 has no tab header or footer.
		String headerText = "";
		String footerText = "";
		if (list.isEmpty() && headerText.length() == 0 && footerText.length() == 0) return;

		int count = list.size();
		int rows = Math.min(20, Math.max(1, count));
		int cols = Math.max(1, (count + rows - 1) / rows);
		boolean numbers = c.tabPingNumbers;
		boolean showPing = !c.tabHidePing;
		int head = c.tabHeads ? 10 : 0;
		int pingW = showPing ? (numbers ? 28 : 14) : 0;
		int scoreW = objective == null ? 0 : 28;
		int nameW = 40;
		for (int i = 0; i < count; i++) {
			nameW = Math.max(nameW, client.textRenderer.getStringWidth(name(list.get(i))));
		}
		int colW = head + nameW + pingW + scoreW + 6;
		int maxCol = Math.max(60, (screenWidth - 16) / cols);
		if (colW > maxCol) colW = maxCol;
		int gridW = colW * cols;
		int headerH = headerText.length() == 0 ? 0 : 12;
		int footerH = footerText.length() == 0 ? 0 : 12;
		int gridH = rows * 9;
		int boxW = Math.max(gridW, Math.max(client.textRenderer.getStringWidth(headerText), client.textRenderer.getStringWidth(footerText))) + 6;
		int boxH = headerH + gridH + footerH + 4;
		int left = screenWidth / 2 - boxW / 2;
		int top = 8;

		OpticHud.fill(left, top, boxW, boxH, OpticHud.withAlpha(c.tabBackgroundColor, 0xB0));
		int y = top + 2;
		if (headerH > 0) {
			int hx = left + boxW / 2 - client.textRenderer.getStringWidth(headerText) / 2;
			client.textRenderer.draw(headerText, hx, y, 0xFF000000 | (c.tabHeaderColor & 0xFFFFFF), c.tabNameShadow);
			y += headerH;
		}
		int gridLeft = left + (boxW - gridW) / 2;
		for (int i = 0; i < count; i++) {
			int col = i / rows;
			int row = i % rows;
			class_482 entry = list.get(i);
			int x = gridLeft + col * colW + 2;
			int rowY = y + row * 9;
			if (c.tabHeads) drawHead(client, entry, x, rowY);
			int textX = x + head;
			String label = name(entry);
			int color = 0xFF000000 | (nameColor(c, client, entry) & 0xFFFFFF);
			client.textRenderer.draw(label, textX, rowY, color, c.tabNameShadow);
			if (objective != null) {
				int value = score(scoreboard, objective, entry);
				if (value != Integer.MIN_VALUE) {
					String scoreText = Integer.toString(value);
					int sx = x + colW - pingW - 4 - client.textRenderer.getStringWidth(scoreText);
					client.textRenderer.draw(scoreText, sx, rowY, 0xFFFFAA00, c.tabNameShadow);
				}
			}
			if (showPing) drawPing(client, c, entry, x + colW - pingW - 1, rowY);
		}
		if (footerH > 0) {
			int fy = y + gridH + 2;
			int fx = left + boxW / 2 - client.textRenderer.getStringWidth(footerText) / 2;
			client.textRenderer.draw(footerText, fx, fy, 0xFF000000 | (c.tabFooterColor & 0xFFFFFF), c.tabNameShadow);
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

	private static void drawPing(MinecraftClient client, OpticConfig c, class_482 entry, int x, int y) {
		int ping = entry.field_1680;
		String label = pingLabel(c, ping);
		if (label == null) {
			client.getTextureManager().bindTexture(DrawableHelper.GUI_ICONS_TEXTURE);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			int filled = bars(ping);
			int index = filled >= 5 ? 0 : filled <= 0 ? 5 : 5 - filled;
			DrawableHelper.drawTexture(x, y, 0.0F, 176.0F + index * 8.0F, 10, 8, 256.0F, 256.0F);
			return;
		}
		if (label.length() == 0) return;
		int color = 0xFF000000 | (pingColor(c, ping) & 0xFFFFFF);
		client.textRenderer.draw(label, x, y, color, c.tabNameShadow);
	}
}
