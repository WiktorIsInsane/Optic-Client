package gg.optic.client.gui;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticFrame;
import gg.optic.client.OpticHudLayout;
import gg.optic.client.OpticUiFont;
import gg.optic.client.hud.OpticHud;
import gg.optic.client.hud.OpticPotions;
import gg.optic.client.hud.OpticTitles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/** Live HUD layout editor: drag real elements, uniform resize, center, open mod config. */
public final class OpticHudEditorScreen extends Screen {
	private static final int ACCENT = 0xFF2DB4E7;
	private static final int BOX = 0x552DB4E7;
	private static final int BOX_ACTIVE = 0x882DB4E7;
	private static final int HANDLE_FILL = 0xFF2DB4E7;

	private final Screen parent;
	private final List<HudItem> items = new ArrayList<HudItem>();
	private HudItem selected;
	private HudItem dragging;
	private boolean resizing;
	private float resizeStartScale;
	private float resizeOriginX;
	private float resizeOriginY;
	private float resizeStartDist;
	private int dragOffX;
	private int dragOffY;
	private int dragKeyIndex = -1;
	private int groupStartX;
	private int groupStartY;
	private int doneX;
	private int doneY;
	private int doneW = 70;
	private int doneH = 18;
	private int centerHX;
	private int centerVX;
	private int centerBX;
	private int toolY;
	private int toolH = 18;
	private int toolW = 72;
	private boolean snapGuideX;
	private boolean snapGuideY;
	private static final int SNAP = 6;

	public OpticHudEditorScreen(Screen parent) {
		this.parent = parent;
	}

	@Override
	public void init() {
		buttons.clear();
		rebuildItems();
		doneX = 10;
		doneY = height - 28;
		toolY = 10;
		centerHX = width - toolW * 3 - 24;
		centerVX = centerHX + toolW + 6;
		centerBX = centerVX + toolW + 6;
		dragging = null;
		resizing = false;
		selected = null;
	}

	private void rebuildItems() {
		items.clear();
		OpticConfig c = OpticClient.get().config();
		if (c.fps) items.add(HudItem.text("FPS", OpticModScreen.MODULE_FPS));
		if (c.cps) items.add(HudItem.text("CPS", OpticModScreen.MODULE_CPS));
		if (c.ping) items.add(HudItem.text("PING", OpticModScreen.MODULE_PING));
		if (c.coordinates) items.add(HudItem.text("XYZ", OpticModScreen.MODULE_COORDINATES));
		if (c.potions) items.add(HudItem.text("POTIONS", OpticModScreen.MODULE_POTIONS));
		if (c.titles) items.add(HudItem.text("TITLES", OpticModScreen.MODULE_TITLES));
		if (c.toggleSprint && c.toggleSprintShowHud) {
			items.add(HudItem.text("SPRINT", OpticModScreen.MODULE_TOGGLE_SPRINT));
		}
		if (c.keystrokes) {
			items.add(HudItem.keystrokes(OpticModScreen.MODULE_KEYSTROKES));
		}
		if (c.scoreboard) {
			items.add(HudItem.box("SCOREBOARD", OpticModScreen.MODULE_SCOREBOARD, false));
		}
		if (c.hotbar) {
			items.add(HudItem.box("HOTBAR", OpticModScreen.MODULE_HOTBAR, true));
		}
		refreshBounds();
	}

	private void refreshBounds() {
		OpticConfig c = OpticClient.get().config();
		Window window = new Window(client, client.width, client.height);
		for (HudItem item : items) {
			if ("KEYSTROKES".equals(item.id)) {
				int[] b = OpticHudLayout.keystrokesBounds(c, client);
				item.x = b[0] - 2;
				item.y = b[1] - 2;
				item.w = b[2] + 4;
				item.h = b[3] + 4;
			} else if ("SCOREBOARD".equals(item.id)) {
				int[] b = OpticHudLayout.scoreboardEditBounds(c, client);
				item.x = b[0] - 2;
				item.y = b[1] - 2;
				item.w = b[2] + 4;
				item.h = b[3] + 4;
			} else if ("HOTBAR".equals(item.id)) {
				item.x = OpticHudLayout.hotbarX(c, window) - 2;
				item.y = OpticHudLayout.hotbarY(c, window) - 2;
				item.w = OpticHudLayout.hotbarDrawW(c) + 4;
				item.h = OpticHudLayout.hotbarDrawH(c) + 4;
			} else if ("FPS".equals(item.id) || "CPS".equals(item.id) || "PING".equals(item.id) || "XYZ".equals(item.id) || "POTIONS".equals(item.id) || "TITLES".equals(item.id)) {
				int[] size = elementSize(item.id, c);
				item.x = posX(item.id, c) - 2;
				item.y = posY(item.id, c) - 2;
				item.w = size[0] + 4;
				item.h = size[1] + 4;
			} else {
				String preview = previewText(item.id, c);
				float scale = scaleOf(item.id, c);
				int textW = measure(preview, usesOptic(item.id, c), scale);
				int textH = Math.max(10, Math.round(OpticHudLayout.fontSize(scale) + 2));
				item.x = posX(item.id, c) - 2;
				item.y = posY(item.id, c) - 2;
				item.w = textW + 4;
				item.h = textH + 4;
			}
			// Resize handle on bottom-left; gear stays on the right.
			item.handleX = item.x;
			item.handleY = item.y + item.h - (int) OpticHudLayout.HANDLE;
			item.gearX = item.x + item.w + 3;
			item.gearY = item.y;
		}
	}

	private int[] elementSize(String id, OpticConfig c) {
		if ("FPS".equals(id)) return OpticHud.fpsSize(client, c);
		if ("CPS".equals(id)) return OpticHud.cpsSize(client, c);
		if ("PING".equals(id)) return OpticHud.pingSize(client, c);
		if ("XYZ".equals(id)) return OpticHud.coordinatesSize(client, c);
		if ("POTIONS".equals(id)) return OpticPotions.size(client, c, true);
		if ("TITLES".equals(id)) return OpticTitles.editorSize(client, c);
		return new int[] { 40, 10 };
	}

	private String previewText(String id, OpticConfig c) {
		if ("FPS".equals(id)) {
			String t = c.fpsShowLabel ? "FPS  " + gg.optic.client.compat.Mc17.fps() : Integer.toString(gg.optic.client.compat.Mc17.fps());
			return OpticConfig.withBrackets(t, c.fpsBrackets);
		}
		if ("CPS".equals(id)) {
			String cpsValue = OpticClient.get().clicks().leftCps() + " | " + OpticClient.get().clicks().rightCps();
			if (!c.cpsSeparateButtons) {
				cpsValue = Integer.toString(OpticClient.get().clicks().leftCps() + OpticClient.get().clicks().rightCps());
			}
			String t = c.cpsShowLabel ? "CPS  " + cpsValue : cpsValue;
			return OpticConfig.withBrackets(t, c.cpsBrackets);
		}
		if ("PING".equals(id)) {
			String value = "0";
			int ping = gg.optic.client.hud.OpticPing.read(client);
			if (ping >= 0) value = Integer.toString(ping);
			if (c.pingShowMs) value = value + "ms";
			String t = c.pingShowLabel ? "PING  " + value : value;
			return OpticConfig.withBrackets(t, c.pingBrackets);
		}
		if ("XYZ".equals(id)) {
			String t = c.coordinatesShowLabel ? "XYZ  0 / 0 / 0" : "0 / 0 / 0";
			return OpticConfig.withBrackets(t, c.coordinatesBrackets);
		}
		if ("SPRINT".equals(id)) {
			return OpticConfig.withBrackets("SPRINT", c.toggleSprintBrackets);
		}
		return id;
	}

	private int measure(String text, boolean optic, float scale) {
		if (optic) {
			return OpticUiFont.widthSized(client, text, 0xFFFFFFFF, OpticHudLayout.fontSize(scale));
		}
		return Math.round(client.textRenderer.getStringWidth(text) * scale);
	}

	private boolean usesOptic(String id, OpticConfig c) {
		if ("FPS".equals(id)) return c.fpsOpticFont;
		if ("CPS".equals(id)) return c.cpsOpticFont;
		if ("PING".equals(id)) return c.pingOpticFont;
		if ("XYZ".equals(id)) return c.coordinatesOpticFont;
		if ("SPRINT".equals(id)) return c.toggleSprintOpticFont;
		return false;
	}

	private float scaleOf(String id, OpticConfig c) {
		if ("FPS".equals(id)) return c.fpsScale;
		if ("CPS".equals(id)) return c.cpsScale;
		if ("PING".equals(id)) return c.pingScale;
		if ("XYZ".equals(id)) return c.coordinatesScale;
		if ("SPRINT".equals(id)) return c.toggleSprintScale;
		if ("POTIONS".equals(id)) return c.potionsScale;
		if ("TITLES".equals(id)) return c.titlesScale;
		if ("KEYSTROKES".equals(id)) return c.keystrokesScale;
		if ("HOTBAR".equals(id)) return c.hotbarScale;
		return 1.0F;
	}

	private void setScale(String id, float scale) {
		OpticConfig c = OpticClient.get().config();
		scale = Math.max(0.5F, Math.min(3.0F, scale));
		if ("FPS".equals(id)) c.fpsScale = scale;
		else if ("CPS".equals(id)) c.cpsScale = scale;
		else if ("PING".equals(id)) c.pingScale = scale;
		else if ("XYZ".equals(id)) c.coordinatesScale = scale;
		else if ("SPRINT".equals(id)) c.toggleSprintScale = scale;
		else if ("POTIONS".equals(id)) c.potionsScale = scale;
		else if ("TITLES".equals(id)) c.titlesScale = scale;
		else if ("KEYSTROKES".equals(id)) c.keystrokesScale = scale;
		else if ("HOTBAR".equals(id)) c.hotbarScale = scale;
	}

	private int posX(String id, OpticConfig c) {
		if ("FPS".equals(id)) return c.fpsX;
		if ("CPS".equals(id)) return c.cpsX;
		if ("PING".equals(id)) return c.pingX;
		if ("XYZ".equals(id)) return c.coordinatesX;
		if ("SPRINT".equals(id)) return c.toggleSprintX;
		if ("POTIONS".equals(id)) {
			int width = OpticPotions.size(client, c, true)[0];
			return OpticPotions.anchorX(c, client, width);
		}
		if ("TITLES".equals(id)) return OpticTitles.editorX(client, c, width);
		if ("SCOREBOARD".equals(id)) {
			return OpticHudLayout.scoreboardEditBounds(c, client)[0];
		}
		if ("HOTBAR".equals(id)) {
			return OpticHudLayout.hotbarX(c, new Window(client, client.width, client.height));
		}
		return 0;
	}

	private int posY(String id, OpticConfig c) {
		if ("FPS".equals(id)) return c.fpsY;
		if ("CPS".equals(id)) return c.cpsY;
		if ("PING".equals(id)) return c.pingY;
		if ("XYZ".equals(id)) return c.coordinatesY;
		if ("SPRINT".equals(id)) return c.toggleSprintY;
		if ("POTIONS".equals(id)) return OpticPotions.anchorY(c);
		if ("TITLES".equals(id)) return OpticTitles.editorY(c, height);
		if ("SCOREBOARD".equals(id)) {
			return OpticHudLayout.scoreboardEditBounds(c, client)[1];
		}
		if ("HOTBAR".equals(id)) {
			return OpticHudLayout.hotbarY(c, new Window(client, client.width, client.height));
		}
		return 0;
	}

	private void setPos(String id, int x, int y) {
		OpticConfig c = OpticClient.get().config();
		if ("FPS".equals(id)) { c.fpsX = x; c.fpsY = y; }
		else if ("CPS".equals(id)) { c.cpsX = x; c.cpsY = y; }
		else if ("PING".equals(id)) { c.pingX = x; c.pingY = y; }
		else if ("XYZ".equals(id)) { c.coordinatesX = x; c.coordinatesY = y; }
		else if ("SPRINT".equals(id)) { c.toggleSprintX = x; c.toggleSprintY = y; }
		else if ("POTIONS".equals(id)) { c.potionsX = x; c.potionsY = y; }
		else if ("TITLES".equals(id)) {
			c.titlesCentered = false;
			c.titlesX = x;
			c.titlesY = y;
		}
		else if ("KEYSTROKES".equals(id)) {
			c.keystrokesX = x;
			c.keystrokesY = y;
		} else if ("SCOREBOARD".equals(id)) {
			c.scoreboardX = x;
			c.scoreboardY = y;
		} else if ("HOTBAR".equals(id)) {
			c.hotbarX = x;
			c.hotbarY = y;
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		OpticConfig c = OpticClient.get().config();
		if (c.keystrokes && c.keystrokesY < 0) {
			c.keystrokesY = OpticHudLayout.keystrokesBaseY(c, client);
		}

		if (dragging != null || resizing) {
			if (!Mouse.isButtonDown(0)) {
				finishInteract();
			} else if (resizing && selected != null) {
				float dx = mouseX - resizeOriginX;
				float dy = mouseY - resizeOriginY;
				float dist = (float) Math.sqrt(dx * dx + dy * dy);
				float factor = dist / Math.max(8.0F, resizeStartDist);
				// Snap to 0.05 steps so scale doesn't jitter every pixel.
				float next = Math.round(resizeStartScale * factor * 20.0F) / 20.0F;
				setScale(selected.id, next);
				refreshBounds();
			} else if (dragging != null) {
				applyDrag(mouseX, mouseY);
				refreshBounds();
			}
		} else {
			snapGuideX = false;
			snapGuideY = false;
			refreshBounds();
		}
		keepOnScreen();

		OpticFrame.begin(client);
		try {
		fill(0, 0, width, height, 0x44000000);
		OpticUiFont.drawSized(client, "EDIT GUI", 10, 10, ACCENT, 12.0F);
		OpticUiFont.drawSized(client, "Drag HUD · center lines lock · corner resize", 10, 26, 0xFFB0B8B0, 8.0F);
		if (snapGuideX) fill(width / 2, 0, width / 2 + 1, height, 0xEEFFFFFF);
		if (snapGuideY) fill(0, height / 2, width, height / 2 + 1, 0xEEFFFFFF);

		for (HudItem item : items) {
			boolean active = item == selected || item == dragging;
			outline(item.x, item.y, item.w, item.h, active ? 0xFF2DB4E7 : 0xAA2DB4E7);
			fill(item.x, item.y, item.x + item.w, item.y + 1, active ? BOX_ACTIVE : BOX);
			if (item.resizable) {
				fill(item.handleX, item.handleY, item.handleX + (int) OpticHudLayout.HANDLE, item.handleY + (int) OpticHudLayout.HANDLE, HANDLE_FILL);
			}
			drawGear(item.gearX, item.gearY, inside(mouseX, mouseY, item.gearX, item.gearY, (int) OpticHudLayout.GEAR, (int) OpticHudLayout.GEAR));
		}

		drawTool(centerHX, "Center H", mouseX, mouseY);
		drawTool(centerVX, "Center V", mouseX, mouseY);
		drawTool(centerBX, "Center", mouseX, mouseY);

		boolean doneHover = inside(mouseX, mouseY, doneX, doneY, doneW, doneH);
		fill(doneX, doneY, doneX + doneW, doneY + doneH, doneHover ? 0xFF2A4A28 : 0xFF1A221A);
		outline(doneX, doneY, doneW, doneH, ACCENT);
		OpticUiFont.drawCenteredSized(client, "DONE", doneX + doneW / 2, doneY + 4, 0xFFFFFFFF, 10.0F);
		} finally {
			OpticFrame.end();
		}
	}

	private void drawTool(int x, String label, int mouseX, int mouseY) {
		boolean hover = inside(mouseX, mouseY, x, toolY, toolW, toolH);
		fill(x, toolY, x + toolW, toolY + toolH, hover ? 0xFF252A30 : 0xFF171A1E);
		outline(x, toolY, toolW, toolH, hover ? ACCENT : 0x66FFFFFF);
		OpticUiFont.drawCenteredSized(client, label, x + toolW / 2, toolY + 4, 0xFFFFFFFF, 8.5F);
	}

	private void drawGear(int x, int y, boolean hover) {
		int s = (int) OpticHudLayout.GEAR;
		fill(x, y, x + s, y + s, hover ? 0xFF2A3038 : 0xFF1A1E22);
		outline(x, y, s, s, ACCENT);
		drawGearIcon(x + s / 2, y + s / 2, s / 2 - 1, 0xFFE8EEF2);
	}

	/** Simple 8-tooth cog drawn with fills. */
	private static void drawGearIcon(int cx, int cy, int radius, int color) {
		int hub = Math.max(2, radius / 2);
		int tooth = Math.max(1, radius / 3);
		for (int i = 0; i < 8; i++) {
			double a = i * Math.PI / 4.0;
			int tx = cx + (int) Math.round(Math.cos(a) * (radius - 1));
			int ty = cy + (int) Math.round(Math.sin(a) * (radius - 1));
			fill(tx - tooth, ty - tooth, tx + tooth + 1, ty + tooth + 1, color);
		}
		fill(cx - hub, cy - hub, cx + hub + 1, cy + hub + 1, color);
		int hole = Math.max(1, hub / 2);
		fill(cx - hole, cy - hole, cx + hole + 1, cy + hole + 1, 0xFF12151A);
	}

	private void applyDrag(int mouseX, int mouseY) {
		OpticConfig c = OpticClient.get().config();
		if ("KEYSTROKES".equals(dragging.id)) {
			if (c.keystrokesLockKeys || dragKeyIndex < 0) {
				int nx = clamp(mouseX - dragOffX, 0, width - 20);
				int ny = clamp(mouseY - dragOffY, 0, height - 20);
				int[] snapped = snapOrigin(nx, ny, dragging.w, dragging.h);
				nx = clamp(snapped[0], 0, Math.max(0, width - Math.max(1, dragging.w - 4)));
				ny = clamp(snapped[1], 0, Math.max(0, height - Math.max(1, dragging.h - 4)));
				int dx = nx - groupStartX;
				int dy = ny - groupStartY;
				c.keystrokesX += dx;
				c.keystrokesY += dy;
				groupStartX = nx;
				groupStartY = ny;
			} else {
				snapGuideX = false;
				snapGuideY = false;
				float scale = Math.max(0.01F, c.keystrokesScale);
				int keyW = OpticHudLayout.keyW(c, dragKeyIndex);
				int keyH = OpticHudLayout.keyH(c, dragKeyIndex);
				int screenX = clamp(mouseX - dragOffX, 0, Math.max(0, width - keyW));
				int screenY = clamp(mouseY - dragOffY, 0, Math.max(0, height - keyH));
				float ox = (screenX - c.keystrokesX) / scale;
				float oy = (screenY - c.keystrokesY) / scale;
				c.setKeyOff(dragKeyIndex, ox, oy);
			}
			return;
		}
		int nx = clamp(mouseX - dragOffX, 0, width - 10);
		int ny = clamp(mouseY - dragOffY, 0, height - 10);
		int[] snapped = snapOrigin(nx, ny, dragging.w, dragging.h);
		// The whole widget stays on screen, not just its corner.
		int cw = Math.max(1, dragging.w - 4);
		int ch = Math.max(1, dragging.h - 4);
		setPos(dragging.id, clamp(snapped[0], 0, Math.max(0, width - cw)), clamp(snapped[1], 0, Math.max(0, height - ch)));
	}

	/**
	 * Pushes any widget that sticks out of the screen back inside (after a resize, a scale
	 * change in the mod menu, or a smaller window). Items already on screen are left alone.
	 */
	private void keepOnScreen() {
		OpticConfig c = OpticClient.get().config();
		boolean moved = false;
		for (HudItem item : items) {
			int left = item.x + 2;
			int top = item.y + 2;
			int cw = Math.max(1, item.w - 4);
			int ch = Math.max(1, item.h - 4);
			int dx = left < 0 ? -left : (left + cw > width ? Math.max(-left, width - cw - left) : 0);
			int dy = top < 0 ? -top : (top + ch > height ? Math.max(-top, height - ch - top) : 0);
			if (dx == 0 && dy == 0) continue;
			if ("KEYSTROKES".equals(item.id)) {
				c.keystrokesX += dx;
				c.keystrokesY += dy;
			} else {
				setPos(item.id, left + dx, top + dy);
			}
			moved = true;
		}
		if (moved) refreshBounds();
	}

	/** Locks the widget center onto the screen's horizontal, vertical, or both midlines. */
	private int[] snapOrigin(int nx, int ny, int boxW, int boxH) {
		snapGuideX = false;
		snapGuideY = false;
		int left = nx - 2;
		int top = ny - 2;
		int midX = width / 2;
		int midY = height / 2;
		if (Math.abs(left + boxW / 2 - midX) <= SNAP) {
			left = midX - boxW / 2;
			snapGuideX = true;
		}
		if (Math.abs(top + boxH / 2 - midY) <= SNAP) {
			top = midY - boxH / 2;
			snapGuideY = true;
		}
		return new int[] { left + 2, top + 2 };
	}

	private void finishInteract() {
		if (dragging != null || resizing) {
			OpticClient.get().config().save();
		}
		dragging = null;
		resizing = false;
		dragKeyIndex = -1;
		snapGuideX = false;
		snapGuideY = false;
	}

	private void centerSelected(boolean horizontal, boolean vertical) {
		if (selected == null) return;
		refreshBounds();
		OpticConfig c = OpticClient.get().config();
		int nx = selected.x + 2;
		int ny = selected.y + 2;
		if (horizontal) nx = Math.max(0, (width - selected.w) / 2 + 2);
		if (vertical) ny = Math.max(0, (height - selected.h) / 2 + 2);
		if ("KEYSTROKES".equals(selected.id)) {
			int[] b = OpticHudLayout.keystrokesBounds(c, client);
			int dx = nx - b[0];
			int dy = ny - b[1];
			c.keystrokesX += dx;
			c.keystrokesY += dy;
		} else {
			setPos(selected.id, nx, ny);
		}
		c.save();
		refreshBounds();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (button != 0) return;
		if (inside(mouseX, mouseY, doneX, doneY, doneW, doneH)) {
			OpticClient.get().config().save();
			client.setScreen(parent);
			return;
		}
		if (inside(mouseX, mouseY, centerHX, toolY, toolW, toolH)) {
			centerSelected(true, false);
			return;
		}
		if (inside(mouseX, mouseY, centerVX, toolY, toolW, toolH)) {
			centerSelected(false, true);
			return;
		}
		if (inside(mouseX, mouseY, centerBX, toolY, toolW, toolH)) {
			centerSelected(true, true);
			return;
		}

		for (int i = items.size() - 1; i >= 0; i--) {
			HudItem item = items.get(i);
			if (inside(mouseX, mouseY, item.gearX, item.gearY, (int) OpticHudLayout.GEAR, (int) OpticHudLayout.GEAR)) {
				OpticClient.get().config().save();
				client.setScreen(OpticModScreen.openModule(item.moduleId));
				return;
			}
			if (item.resizable && inside(mouseX, mouseY, item.handleX, item.handleY, (int) OpticHudLayout.HANDLE, (int) OpticHudLayout.HANDLE)) {
				selected = item;
				resizing = true;
				resizeStartScale = scaleOf(item.id, OpticClient.get().config());
				// Fixed top-right (opposite the bottom-left handle) — does not move while scaling.
				resizeOriginX = item.x + item.w;
				resizeOriginY = item.y;
				float dx = mouseX - resizeOriginX;
				float dy = mouseY - resizeOriginY;
				resizeStartDist = Math.max(8.0F, (float) Math.sqrt(dx * dx + dy * dy));
				return;
			}
			if (inside(mouseX, mouseY, item.x, item.y, item.w, item.h)) {
				selected = item;
				dragging = item;
				OpticConfig c = OpticClient.get().config();
				if ("KEYSTROKES".equals(item.id) && !c.keystrokesLockKeys) {
					dragKeyIndex = hitKeyIndex(mouseX, mouseY, c);
					if (dragKeyIndex >= 0) {
						dragOffX = mouseX - OpticHudLayout.keyX(c, client, dragKeyIndex);
						dragOffY = mouseY - OpticHudLayout.keyY(c, client, dragKeyIndex);
					} else {
						dragOffX = mouseX - c.keystrokesX;
						dragOffY = mouseY - OpticHudLayout.keystrokesBaseY(c, client);
						groupStartX = c.keystrokesX;
						groupStartY = OpticHudLayout.keystrokesBaseY(c, client);
					}
				} else if ("KEYSTROKES".equals(item.id)) {
					dragKeyIndex = -1;
					dragOffX = mouseX - c.keystrokesX;
					dragOffY = mouseY - OpticHudLayout.keystrokesBaseY(c, client);
					groupStartX = c.keystrokesX;
					groupStartY = OpticHudLayout.keystrokesBaseY(c, client);
				} else {
					dragOffX = mouseX - posX(item.id, c);
					dragOffY = mouseY - posY(item.id, c);
				}
				return;
			}
		}
		selected = null;
	}

	private int hitKeyIndex(int mouseX, int mouseY, OpticConfig c) {
		int count = c.keystrokesShowMouse ? 6 : 4;
		for (int i = count - 1; i >= 0; i--) {
			int x = OpticHudLayout.keyX(c, client, i);
			int y = OpticHudLayout.keyY(c, client, i);
			int w = OpticHudLayout.keyW(c, i);
			int h = OpticHudLayout.keyH(c, i);
			if (inside(mouseX, mouseY, x, y, w, h)) return i;
		}
		return -1;
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RSHIFT) {
			finishInteract();
			OpticClient.get().config().save();
			client.setScreen(parent);
			return;
		}
		super.keyPressed(character, keyCode);
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	private static void outline(int x, int y, int w, int h, int color) {
		fill(x, y, x + w, y + 1, color);
		fill(x, y + h - 1, x + w, y + h, color);
		fill(x, y, x + 1, y + h, color);
		fill(x + w - 1, y, x + w, y + h, color);
	}

	private static final class HudItem {
		final String id;
		final int moduleId;
		final boolean resizable;
		int x;
		int y;
		int w;
		int h;
		int handleX;
		int handleY;
		int gearX;
		int gearY;

		private HudItem(String id, int moduleId, boolean resizable) {
			this.id = id;
			this.moduleId = moduleId;
			this.resizable = resizable;
		}

		static HudItem text(String id, int moduleId) {
			return new HudItem(id, moduleId, true);
		}

		static HudItem keystrokes(int moduleId) {
			return new HudItem("KEYSTROKES", moduleId, true);
		}

		static HudItem box(String id, int moduleId, boolean resizable) {
			return new HudItem(id, moduleId, resizable);
		}
	}
}
