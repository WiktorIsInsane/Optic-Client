package gg.optic.client.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticHudLayout;
import gg.optic.client.gui.OpticHudEditorScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Movable potion status list. Hides the vanilla inventory panel when asked. */
public final class OpticPotions {
	private static final Identifier INVENTORY_TEXTURE = new Identifier("textures/gui/container/inventory.png");

	private OpticPotions() {
	}

	public static boolean hideVanilla() {
		OpticConfig c = OpticClient.get().config();
		return c.potions && c.potionsHideVanilla;
	}

	public static void render(MinecraftClient client) {
		OpticConfig c = OpticClient.get().config();
		if (!c.potions || client.player == null || client.options.hudHidden) return;
		Screen screen = client.currentScreen;
		boolean editor = screen instanceof OpticHudEditorScreen;
		if (screen != null && !(screen instanceof ChatScreen) && !editor) {
			boolean inventory = screen instanceof HandledScreen || screen instanceof InventoryScreen;
			if (!inventory || !c.potionsShowInInventory) return;
		}
		List<Row> rows = rows(client.player, c);
		if (rows.isEmpty() && editor) rows = previewRows(c);
		if (rows.isEmpty()) return;
		draw(client, c, rows);
	}

	public static int[] size(MinecraftClient client, OpticConfig c, boolean preview) {
		List<Row> rows = preview || client.player == null ? previewRows(c) : rows(client.player, c);
		if (rows.isEmpty()) rows = previewRows(c);
		return measure(client, c, rows);
	}

	public static int anchorX(OpticConfig c, MinecraftClient client, int width) {
		if (c.potionsX >= 0) return c.potionsX;
		int screen = new net.minecraft.client.util.Window(client).getWidth();
		return Math.max(2, screen - width - 4);
	}

	public static int anchorY(OpticConfig c) {
		return c.potionsY >= 0 ? c.potionsY : 4;
	}

	private static void draw(MinecraftClient client, OpticConfig c, List<Row> rows) {
		int[] size = measure(client, c, rows);
		int x = anchorX(c, client, size[0]);
		int y = anchorY(c);
		if (c.potionsBackground) {
			OpticHud.fill(x - 3, y - 2, size[0] + 6, size[1] + 4, OpticHud.withAlpha(c.potionsBackgroundColor, 0x90));
		}
		float line = OpticHudLayout.fontSize(c.potionsScale) + 2.0F;
		if (c.potionsHorizontal) {
			int cursor = x;
			for (int i = 0; i < rows.size(); i++) {
				Row row = rows.get(i);
				int col = columnWidth(client, c, row);
				int icon = iconSize(c, row);
				if (icon > 0) drawIcon(client, row.icon, cursor, y, icon);
				int textX = cursor + iconOffset(c, row);
				drawLine(client, c, row.title, textX, y, c.potionsTextColor);
				if (row.time.length() > 0) drawLine(client, c, row.time, textX, y + Math.round(line), timeColor(c, row));
				cursor += col + 8;
			}
			return;
		}
		int cursorY = y;
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			int icon = iconSize(c, row);
			if (icon > 0) drawIcon(client, row.icon, x, cursorY, icon);
			int textX = x + iconOffset(c, row);
			drawLine(client, c, row.title, textX, cursorY, c.potionsTextColor);
			cursorY += Math.round(line);
			if (row.time.length() > 0) {
				drawLine(client, c, row.time, textX, cursorY, timeColor(c, row));
				cursorY += Math.round(line);
			}
		}
	}

	/** Vanilla effect icon from the inventory sheet, scaled to cover the row's text lines. */
	private static void drawIcon(MinecraftClient client, int iconLevel, int x, int y, int size) {
		client.getTextureManager().bindTexture(INVENTORY_TEXTURE);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, 0.0F);
		float s = size / 18.0F;
		GlStateManager.scale(s, s, 1.0F);
		DrawableHelper.drawTexture(0, 0, iconLevel % 8 * 18.0F, 198.0F + iconLevel / 8 * 18.0F, 18, 18, 256.0F, 256.0F);
		GlStateManager.popMatrix();
	}

	private static int iconSize(OpticConfig c, Row row) {
		if (!c.potionsSymbols || row.icon < 0) return 0;
		float line = OpticHudLayout.fontSize(c.potionsScale) + 2.0F;
		return Math.max(8, Math.round(line * (row.time.length() > 0 ? 2 : 1)) - 2);
	}

	private static int iconOffset(OpticConfig c, Row row) {
		int size = iconSize(c, row);
		return size > 0 ? size + 3 : 0;
	}

	/** Remaining time turns red once the effect is under the warning threshold. */
	private static int timeColor(OpticConfig c, Row row) {
		return row.expiring ? 0xFF5555 : c.potionsTextColor;
	}

	private static void drawLine(MinecraftClient client, OpticConfig c, String text, int x, int y, int color) {
		if (text == null || text.length() == 0) return;
		List<OpticHud.Piece> pieces = new ArrayList<OpticHud.Piece>();
		pieces.add(new OpticHud.Piece(text, color));
		OpticHud.drawPieces(client, pieces, x, y, c.potionsSymbols, c.potionsTextShadow, c.potionsScale);
	}

	private static int[] measure(MinecraftClient client, OpticConfig c, List<Row> rows) {
		float line = OpticHudLayout.fontSize(c.potionsScale) + 2.0F;
		if (c.potionsHorizontal) {
			int width = 0;
			int lines = 1;
			for (int i = 0; i < rows.size(); i++) {
				if (i > 0) width += 8;
				width += columnWidth(client, c, rows.get(i));
				if (rows.get(i).time.length() > 0) lines = 2;
			}
			return new int[] { Math.max(8, width), Math.max(8, Math.round(lines * line)) };
		}
		int width = 0;
		int count = 0;
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			width = Math.max(width, columnWidth(client, c, row));
			count += row.time.length() > 0 ? 2 : 1;
		}
		return new int[] { Math.max(8, width), Math.max(8, Math.round(count * line)) };
	}

	private static int columnWidth(MinecraftClient client, OpticConfig c, Row row) {
		int width = textWidth(client, c, row.title);
		if (row.time.length() > 0) width = Math.max(width, textWidth(client, c, row.time));
		return width + iconOffset(c, row);
	}

	private static int textWidth(MinecraftClient client, OpticConfig c, String text) {
		if (c.potionsSymbols) {
			return gg.optic.client.OpticUiFont.widthSized(client, text, 0xFFFFFFFF, OpticHudLayout.fontSize(c.potionsScale));
		}
		return Math.round(client.textRenderer.getStringWidth(text) * c.potionsScale);
	}

	private static List<Row> previewRows(OpticConfig c) {
		List<Row> rows = new ArrayList<Row>();
		rows.add(row(c, 16, "Night Vision", 0, "3:00"));
		rows.add(row(c, 1, "Speed", 1, "1:20"));
		return rows;
	}

	private static List<Row> rows(ClientPlayerEntity player, OpticConfig c) {
		Collection<StatusEffectInstance> effects = player.getStatusEffectInstances();
		List<StatusEffectInstance> list = new ArrayList<StatusEffectInstance>();
		if (effects != null) list.addAll(effects);
		Collections.sort(list, new Comparator<StatusEffectInstance>() {
			@Override
			public int compare(StatusEffectInstance a, StatusEffectInstance b) {
				return a.getEffectId() - b.getEffectId();
			}
		});
		List<Row> rows = new ArrayList<Row>();
		for (int i = 0; i < list.size(); i++) {
			StatusEffectInstance instance = list.get(i);
			StatusEffect type = effect(instance.getEffectId());
			if (type == null) continue;
			boolean expiring = c.potionsBlink && instance.getDuration() > 0 && instance.getDuration() < c.potionsBlinkSeconds * 20;
			String name = I18n.translate(type.getTranslationKey());
			String duration = instance.getDuration() > 32000 ? "**:**" : StatusEffect.getFormattedDuration(instance);
			rows.add(row(c, instance.getEffectId(), name, instance.getAmplifier(), duration, expiring));
		}
		return rows;
	}

	private static Row row(OpticConfig c, int effectId, String name, int amplifier, String duration) {
		return row(c, effectId, name, amplifier, duration, false);
	}

	private static Row row(OpticConfig c, int effectId, String name, int amplifier, String duration, boolean expiring) {
		StringBuilder title = new StringBuilder();
		if (c.potionsShowName) title.append(name);
		if (c.potionsShowAmplifier && amplifier >= 0) {
			if (title.length() > 0 && title.charAt(title.length() - 1) != ' ') title.append(' ');
			title.append(level(amplifier));
		}
		if (title.length() == 0) title.append(name);
		String time = c.potionsShowDuration ? duration : "";
		StatusEffect type = effect(effectId);
		int icon = type != null && type.hasIcon() ? type.getIconLevel() : -1;
		return new Row(title.toString(), time, icon, expiring);
	}

	private static final class Row {
		private final String title;
		private final String time;
		private final int icon;
		private final boolean expiring;

		private Row(String title, String time, int icon, boolean expiring) {
			this.title = title;
			this.time = time == null ? "" : time;
			this.icon = icon;
			this.expiring = expiring;
		}
	}

	private static String level(int amplifier) {
		switch (amplifier) {
			case 0: return "I";
			case 1: return "II";
			case 2: return "III";
			case 3: return "IV";
			case 4: return "V";
			default: return Integer.toString(amplifier + 1);
		}
	}

	private static StatusEffect effect(int id) {
		StatusEffect[] all = StatusEffect.STATUS_EFFECTS;
		if (all == null || id < 0 || id >= all.length) return null;
		return all[id];
	}
}
