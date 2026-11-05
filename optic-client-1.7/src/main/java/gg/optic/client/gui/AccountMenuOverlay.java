package gg.optic.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticTextures;
import gg.optic.client.OpticUiDraw;
import gg.optic.client.OpticUiFont;
import gg.optic.client.account.AccountHeads;
import gg.optic.client.account.MicrosoftAuth;
import gg.optic.client.account.SessionSwap;
import gg.optic.client.account.SharedAccount;
import gg.optic.client.account.SharedAccounts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.Texture;
import net.minecraft.client.util.Session;
import net.minecraft.util.Identifier;
import org.lwjgl.input.Keyboard;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Top-left account chip + dropdown (hot-swap, sign-in offline/Microsoft). The close X stays top-right. */
public final class AccountMenuOverlay {
	private static final int ACCENT = OpticTheme.ACCENT;
	private static final int ACCENT_HOVER = 0xFF3DB86A;
	private static final int BACK = 0xFFFF4D4D;
	private static final int CHIP = OpticMenuChrome.GLASS_20;
	private static final int CHIP_HOT = OpticMenuChrome.GLASS_20_HOT;
	private static final int PANEL = OpticTheme.PANEL;
	private static final int HEAD = 14;
	private static final int CHEV = 7;
	/** Space between the head and the name on the chip. */
	private static final int HEAD_GAP = 4;
	private static final float NAME_SIZE = 8.0F;
	private static final float CHIP_KIND_SIZE = 6.5F;
	private static final int CHIP_H = OpticMenuChrome.BAR_H;
	private static final int CHIP_Y = OpticMenuChrome.BAR_Y;
	private static Identifier chevronIcon;
	private static Identifier removeMark;
	private static final int ROW_H = 26;
	private static final int ROW_HEAD = 14;
	private static final int HEADER_H = 42;
	private static final int MENU_W = 200;
	private static final float ROW_SIZE = 8.0F;
	private static final float KIND_SIZE = 7.0F;
	private static final int INK = 0xFF07110A;
	/** Room for the close button plus the gap between the two boxes. */
	private static final int RIGHT_GUTTER = OpticMenuChrome.BAR_EDGE + OpticMenuChrome.BAR_H + OpticMenuChrome.BAR_GAP;

	private enum Mode {
		CLOSED,
		LIST,
		SIGN_IN,
		OFFLINE,
		MICROSOFT
	}

	private Mode mode = Mode.CLOSED;
	private SharedAccounts.Snapshot snap = SharedAccounts.Snapshot.empty();
	private long lastReloadMs;
	private String status = "";
	private long statusUntil;
	private String msStatus = "";
	private String msCode = "";
	private boolean msBusy;
	private String offlineName = "";

	public void reload() {
		snap = SharedAccounts.load();
		lastReloadMs = System.currentTimeMillis();
	}

	public int chipWidth(MinecraftClient client) {
		SharedAccount active = resolveActive(client);
		String name = displayName(client, active);
		int nameW = OpticUiFont.widthSharp(client, name, 0xFFFFFFFF, NAME_SIZE);
		return Math.min(240, 3 + HEAD + HEAD_GAP + nameW + 4);
	}

	/** Pinned to the top-left corner, level with the close button on the right. */
	public int chipX(MinecraftClient client, int screenWidth) {
		return OpticMenuChrome.BAR_EDGE;
	}

	public void render(MinecraftClient client, int screenWidth, int mouseX, int mouseY) {
		if (System.currentTimeMillis() - lastReloadMs > 1500L) reload();

		SharedAccount active = resolveActive(client);
		String name = displayName(client, active);
		String uuid = active != null ? active.uuid : client.getSession().getUuid();

		int chipW = chipWidth(client);
		int x = chipX(client, screenWidth);
		int y = CHIP_Y;
		boolean hot = inside(mouseX, mouseY, x, y, chipW, CHIP_H);

		// No background at all: head, name, account type in accent, chevron. Name and chevron go accent on hover.
		boolean open = mode != Mode.CLOSED;
		if (mode == Mode.LIST) {
			// Open: the chip becomes the header of one panel with the list under it.
			drawList(client, active, x, chipW, y, mouseX, mouseY);
		}
		int headX = x + 3;
		int headY = y + (CHIP_H - HEAD) / 2;
		drawHead(client, uuid, active != null ? active.username : client.getSession().getUsername(), headX, headY, HEAD);
		int nameX = headX + HEAD + HEAD_GAP;
		int nameH = OpticUiFont.heightSharp(client, name, 0xFFFFFFFF, NAME_SIZE);
		OpticUiFont.drawSharp(client, name, nameX, y + (CHIP_H - nameH) / 2, mode == Mode.LIST ? 0xFFFFFFFF : hot || open ? ACCENT : OpticTheme.TEXT, NAME_SIZE);
		if (mode == Mode.LIST) {
			int pw = panelWidth(client, active, chipW);
			drawIcon(client, chevron(client), x + pw - 7 - CHEV, y + (CHIP_H - CHEV) / 2, CHEV, 0xFFFFFFFF, true);
		}

		if (mode == Mode.LIST) {
			// drawn above, under the header text
		} else if (mode == Mode.SIGN_IN) {
			drawAdd(client, x, chipW, y, mouseX, mouseY);
		} else if (mode == Mode.OFFLINE) {
			drawOffline(client, x, chipW, y, mouseX, mouseY);
		} else if (mode == Mode.MICROSOFT) {
			drawMicrosoft(client, x, chipW, y);
		}

		if (statusUntil > System.currentTimeMillis() && !status.isEmpty() && mode == Mode.CLOSED) {
			OpticUiFont.drawSharp(client, status, x, y + CHIP_H + 6, ACCENT, 9.0F);
		}
	}

	/** @return true if the click was consumed */
	public boolean mouseClicked(MinecraftClient client, int mouseX, int mouseY, int screenWidth) {
		if (System.currentTimeMillis() - lastReloadMs > 1500L) reload();
		SharedAccount active = resolveActive(client);
		int chipW = chipWidth(client);
		int x = chipX(client, screenWidth);
		int y = CHIP_Y;

		int headerW = mode == Mode.LIST ? panelWidth(client, active, chipW) : chipW;
		if (inside(mouseX, mouseY, x, y, headerW, CHIP_H)) {
			mode = mode == Mode.CLOSED ? Mode.LIST : Mode.CLOSED;
			return true;
		}

		if (mode == Mode.LIST) {
			return clickList(client, active, x, chipW, y, mouseX, mouseY);
		}
		if (mode == Mode.SIGN_IN) {
			return clickAdd(client, x, chipW, y, mouseX, mouseY);
		}
		if (mode == Mode.OFFLINE) {
			return clickOffline(client, x, chipW, y, mouseX, mouseY);
		}
		if (mode == Mode.MICROSOFT) {
			if (!msBusy) mode = Mode.SIGN_IN;
			return true;
		}
		return false;
	}

	public boolean keyPressed(MinecraftClient client, char typed, int key) {
		if (mode == Mode.CLOSED) return false;
		if (key == Keyboard.KEY_ESCAPE) {
			if (mode == Mode.OFFLINE) mode = Mode.SIGN_IN;
			else if (mode == Mode.SIGN_IN || mode == Mode.MICROSOFT) mode = Mode.LIST;
			else mode = Mode.CLOSED;
			return true;
		}
		if (mode != Mode.OFFLINE) return true;
		if (key == Keyboard.KEY_RETURN) {
			confirmOffline(client);
			return true;
		}
		if (key == Keyboard.KEY_BACK && offlineName.length() > 0) {
			offlineName = offlineName.substring(0, offlineName.length() - 1);
			return true;
		}
		if (typed >= 32 && typed < 127 && offlineName.length() < 16) {
			offlineName += typed;
		}
		return true;
	}

	public boolean confirmOffline(MinecraftClient client) {
		if (mode != Mode.OFFLINE) return false;
		String name = offlineName == null ? "" : offlineName.trim();
		try {
			SharedAccount created = gg.optic.client.account.OfflineAccounts.create(name);
			SessionSwap.applyNew(client, created);
			offlineName = "";
			reload();
			mode = Mode.CLOSED;
			toast("Signed in as " + created.username);
		} catch (RuntimeException e) {
			toast(e.getMessage() == null ? "Could not add account" : e.getMessage());
		}
		return true;
	}

	/** Open-list metrics: one dark panel, header (the chip) on top, rows, then "Add account". */
	private static final int LIST_ROW = 20;
	private static final int LIST_HEAD = 14;
	private static final int LIST_X = 11;
	private static final int LIST_BG = 0xF2262628;
	private static final int LIST_HEADER = 0xFF3A3A3D;
	private static final int LIST_EDGE = 0xFF505054;
	private static final int ADD_BLUE = 0xFF3FA9F5;

	private int panelWidth(MinecraftClient client, SharedAccount active, int chipW) {
		int w = Math.max(chipW + CHEV + 10, 120);
		for (SharedAccount account : others(active)) {
			String ign = account.username == null ? "" : account.username;
			int need = 5 + LIST_HEAD + 6 + OpticUiFont.widthSharp(client, ign, 0xFFFFFFFF, 9.0F) + 8 + LIST_X + 6;
			if (need > w) w = need;
		}
		return Math.min(MENU_W, w);
	}

	private void drawList(MinecraftClient client, SharedAccount active, int chipX, int chipW, int chipY, int mouseX, int mouseY) {
		List<SharedAccount> others = others(active);
		int pw = panelWidth(client, active, chipW);
		int bodyY = chipY + CHIP_H;
		int totalH = CHIP_H + others.size() * LIST_ROW + LIST_ROW + 2;
		OpticUiDraw.fillExact(client, chipX, chipY, pw, totalH, 5, LIST_BG);
		OpticUiDraw.fillExact(client, chipX, chipY, pw, CHIP_H, 5, LIST_HEADER);
		fill(chipX, bodyY - 5, chipX + pw, bodyY, LIST_HEADER);
		fill(chipX, bodyY, chipX + pw, bodyY + 1, LIST_EDGE);
		OpticUiDraw.outlineExact(client, chipX, chipY, pw, totalH, 5, LIST_EDGE);

		int rowY = bodyY + 1;
		for (SharedAccount account : others) {
			boolean rowHot = inside(mouseX, mouseY, chipX + 1, rowY, pw - 2, LIST_ROW);
			int xX = chipX + pw - 6 - LIST_X;
			int xY = rowY + (LIST_ROW - LIST_X) / 2;
			boolean xHot = inside(mouseX, mouseY, xX, xY, LIST_X, LIST_X);
			if (rowHot && !xHot) fill(chipX + 1, rowY, chipX + pw - 1, rowY + LIST_ROW, 0x16FFFFFF);
			drawHead(client, account.uuid, account.username, chipX + 5, rowY + (LIST_ROW - LIST_HEAD) / 2, LIST_HEAD);
			String ign = fitText(client, account.username == null ? "" : account.username, pw - 5 - LIST_HEAD - 6 - LIST_X - 12, 9.0F);
			int th = OpticUiFont.heightSharp(client, ign, 0xFFFFFFFF, 9.0F);
			OpticUiFont.drawSharp(client, ign, chipX + 5 + LIST_HEAD + 6, rowY + (LIST_ROW - th) / 2, 0xFFFFFFFF, 9.0F);
			// Remove: small outlined square with an X.
			if (xHot) fill(xX, xY, xX + LIST_X, xY + LIST_X, 0x30FFFFFF);
			OpticUiDraw.outlineExact(client, xX, xY, LIST_X, LIST_X, 2, xHot ? 0xFFFFFFFF : 0xFFBDBDC2);
			drawIcon(client, removeIcon(client), xX + 1, xY + 1, LIST_X - 2, xHot ? 0xFFFFFFFF : 0xFFE0E0E4, false);
			rowY += LIST_ROW;
		}
		String add = "Add account";
		boolean addHot = inside(mouseX, mouseY, chipX + 1, rowY, pw - 2, LIST_ROW);
		if (addHot) fill(chipX + 1, rowY, chipX + pw - 1, rowY + LIST_ROW, 0x10FFFFFF);
		int aw = OpticUiFont.widthSharp(client, add, ADD_BLUE, 9.0F);
		int ah = OpticUiFont.heightSharp(client, add, ADD_BLUE, 9.0F);
		OpticUiFont.drawSharp(client, add, chipX + (pw - aw) / 2, rowY + (LIST_ROW - ah) / 2, addHot ? 0xFF7CC6FF : ADD_BLUE, 9.0F);
	}

	private boolean clickList(MinecraftClient client, SharedAccount active, int chipX, int chipW, int chipY, int mouseX, int mouseY) {
		List<SharedAccount> others = others(active);
		int pw = panelWidth(client, active, chipW);
		int totalH = CHIP_H + others.size() * LIST_ROW + LIST_ROW + 2;
		int rowY = chipY + CHIP_H + 1;
		for (SharedAccount account : others) {
			int xX = chipX + pw - 6 - LIST_X;
			int xY = rowY + (LIST_ROW - LIST_X) / 2;
			if (inside(mouseX, mouseY, xX - 2, xY - 2, LIST_X + 4, LIST_X + 4)) {
				SharedAccounts.remove(account.id);
				reload();
				return true;
			}
			if (inside(mouseX, mouseY, chipX, rowY, pw, LIST_ROW)) {
				SessionSwap.apply(client, account);
				reload();
				mode = Mode.CLOSED;
				toast("Signed in as " + account.username);
				return true;
			}
			rowY += LIST_ROW;
		}
		if (inside(mouseX, mouseY, chipX, rowY, pw, LIST_ROW)) {
			mode = Mode.SIGN_IN;
			return true;
		}
		if (!inside(mouseX, mouseY, chipX, chipY, pw, totalH)) mode = Mode.CLOSED;
		return true;
	}
	private void drawAdd(MinecraftClient client, int chipX, int chipW, int chipY, int mouseX, int mouseY) {
		int extra = msStatus.isEmpty() ? 0 : 28;
		int menuH = 8 + 18 + 28 + 28 + extra;
		int menuX = menuX(chipX, chipW, MENU_W);
		int menuY = chipY + CHIP_H + 6;
		OpticUiDraw.fillExact(client, menuX, menuY, MENU_W, menuH, 8, PANEL);
		OpticUiDraw.outlineExact(client, menuX, menuY, MENU_W, menuH, 8, OpticTheme.BORDER);
		boolean backHot = inside(mouseX, mouseY, menuX, menuY + 4, MENU_W, 16);
		OpticUiFont.drawSharp(client, "Back", menuX + 10, menuY + 6, backHot ? 0xFFFFFFFF : 0xFF9A9AA8, KIND_SIZE);

		int msY = menuY + 24;
		boolean msHot = inside(mouseX, mouseY, menuX + 8, msY, MENU_W - 16, 24);
		OpticUiDraw.fillExact(client, menuX + 8, msY, MENU_W - 16, 24, 6, msHot ? ACCENT_HOVER : ACCENT);
		String ms = "Microsoft account";
		int msW = OpticUiFont.widthSharp(client, ms, INK, ROW_SIZE);
		OpticUiFont.drawSharp(client, ms, menuX + (MENU_W - msW) / 2, msY + 7, INK, ROW_SIZE);

		int offY = msY + 28;
		boolean offHot = inside(mouseX, mouseY, menuX + 8, offY, MENU_W - 16, 24);
		OpticUiDraw.fillExact(client, menuX + 8, offY, MENU_W - 16, 24, 6, offHot ? 0xFF1C1C22 : 0xFF141418);
		OpticUiDraw.outlineExact(client, menuX + 8, offY, MENU_W - 16, 24, 6, OpticTheme.BORDER);
		String off = "Offline account";
		int offW = OpticUiFont.widthSharp(client, off, 0xFFFFFFFF, ROW_SIZE);
		OpticUiFont.drawSharp(client, off, menuX + (MENU_W - offW) / 2, offY + 7, 0xFFFFFFFF, ROW_SIZE);
		if (!msStatus.isEmpty()) {
			String line = msCode.isEmpty() ? msStatus : msStatus + " " + msCode;
			OpticUiFont.drawSharp(client, fitText(client, line, MENU_W - 20, KIND_SIZE), menuX + 10, offY + 30, 0xFF9A9AA8, KIND_SIZE);
		}
	}

	private boolean clickAdd(MinecraftClient client, int chipX, int chipW, int chipY, int mouseX, int mouseY) {
		int extra = msStatus.isEmpty() ? 0 : 28;
		int menuH = 8 + 18 + 28 + 28 + extra;
		int menuX = menuX(chipX, chipW, MENU_W);
		int menuY = chipY + CHIP_H + 6;
		if (inside(mouseX, mouseY, menuX, menuY + 4, MENU_W, 16)) {
			mode = Mode.LIST;
			return true;
		}
		int msY = menuY + 24;
		if (inside(mouseX, mouseY, menuX + 8, msY, MENU_W - 16, 24)) {
			startMicrosoft(client);
			return true;
		}
		int offY = msY + 28;
		if (inside(mouseX, mouseY, menuX + 8, offY, MENU_W - 16, 24)) {
			offlineName = "";
			mode = Mode.OFFLINE;
			return true;
		}
		if (!inside(mouseX, mouseY, menuX, menuY, MENU_W, menuH)) mode = Mode.CLOSED;
		return true;
	}

	private void drawOffline(MinecraftClient client, int chipX, int chipW, int chipY, int mouseX, int mouseY) {
		int menuH = 92;
		int menuX = menuX(chipX, chipW, MENU_W);
		int menuY = chipY + CHIP_H + 6;
		OpticUiDraw.fillExact(client, menuX, menuY, MENU_W, menuH, 8, PANEL);
		OpticUiDraw.outlineExact(client, menuX, menuY, MENU_W, menuH, 8, OpticTheme.BORDER);
		boolean backHot = inside(mouseX, mouseY, menuX, menuY + 4, MENU_W, 16);
		OpticUiFont.drawSharp(client, "Back", menuX + 10, menuY + 6, backHot ? 0xFFFFFFFF : 0xFF9A9AA8, KIND_SIZE);
		OpticUiDraw.fillExact(client, menuX + 8, menuY + 26, MENU_W - 16, 22, 6, 0xFF101014);
		OpticUiDraw.outlineExact(client, menuX + 8, menuY + 26, MENU_W - 16, 22, 6, ACCENT);
		String shown = offlineName.isEmpty() ? "Username" : offlineName;
		int color = offlineName.isEmpty() ? 0xFF9A9AA8 : 0xFFFFFFFF;
		OpticUiFont.drawSharp(client, shown, menuX + 14, menuY + 32, color, ROW_SIZE);
		boolean hot = inside(mouseX, mouseY, menuX + 8, menuY + 54, MENU_W - 16, 24);
		OpticUiDraw.fillExact(client, menuX + 8, menuY + 54, MENU_W - 16, 24, 6, hot ? ACCENT_HOVER : ACCENT);
		String confirm = "Confirm";
		int confirmW = OpticUiFont.widthSharp(client, confirm, INK, ROW_SIZE);
		OpticUiFont.drawSharp(client, confirm, menuX + (MENU_W - confirmW) / 2, menuY + 61, INK, ROW_SIZE);
	}

	private boolean clickOffline(MinecraftClient client, int chipX, int chipW, int chipY, int mouseX, int mouseY) {
		int menuX = menuX(chipX, chipW, MENU_W);
		int menuY = chipY + CHIP_H + 6;
		if (inside(mouseX, mouseY, menuX, menuY + 4, MENU_W, 16)) {
			mode = Mode.SIGN_IN;
			return true;
		}
		if (inside(mouseX, mouseY, menuX + 8, menuY + 54, MENU_W - 16, 24)) {
			confirmOffline(client);
			return true;
		}
		if (!inside(mouseX, mouseY, menuX, menuY, MENU_W, 92)) mode = Mode.CLOSED;
		return true;
	}

	private void drawMicrosoft(MinecraftClient client, int chipX, int chipW, int chipY) {
		int menuX = menuX(chipX, chipW, MENU_W);
		int menuY = chipY + CHIP_H + 6;
		OpticUiDraw.fillExact(client, menuX, menuY, MENU_W, 58, 8, PANEL);
		OpticUiDraw.outlineExact(client, menuX, menuY, MENU_W, 58, 8, OpticTheme.BORDER);
		OpticUiFont.drawSharp(client, "Microsoft sign-in", menuX + 10, menuY + 8, 0xFFFFFFFF, 9.0F);
		String line = msStatus.isEmpty() ? "Starting..." : msStatus;
		if (!msCode.isEmpty()) line = line + " " + msCode;
		OpticUiFont.drawSharp(client, fitText(client, line, MENU_W - 20, KIND_SIZE), menuX + 10, menuY + 24, 0xFF9A9AA8, KIND_SIZE);
		OpticUiFont.drawSharp(client, msBusy ? "Waiting in the browser" : "Click to go back", menuX + 10, menuY + 40, 0xFF777777, KIND_SIZE);
	}

	private void drawTextRow(MinecraftClient client, String label, int menuX, int rowY, int menuW, int mouseX, int mouseY, int color) {
		if (inside(mouseX, mouseY, menuX + 4, rowY, menuW - 8, ROW_H)) {
			OpticUiDraw.fillExact(client, menuX + 4, rowY, menuW - 8, ROW_H, 4, 0x14FFFFFF);
		}
		OpticUiFont.drawSharp(client, label, menuX + 10, rowY + 8, color, ROW_SIZE);
	}

	private void signOut(MinecraftClient client, SharedAccount active) {
		SharedAccounts.remove(active.id);
		reload();
		SharedAccount next = snap.active();
		if (next != null) {
			SessionSwap.apply(client, next);
			toast("Signed in as " + next.username);
		} else {
			toast("Signed out");
		}
		mode = Mode.CLOSED;
	}

	private static boolean savedAccount(SharedAccount active) {
		return active != null && active.kind != null && !"session".equalsIgnoreCase(active.kind);
	}

	private static String kindLabel(SharedAccount account) {
		if (account == null || account.kind == null || account.kind.isEmpty() || "session".equalsIgnoreCase(account.kind)) {
			return "Guest";
		}
		if ("microsoft".equalsIgnoreCase(account.kind)) return "Microsoft";
		if ("offline".equalsIgnoreCase(account.kind)) return "Offline";
		return account.kind;
	}

	private void startMicrosoft(final MinecraftClient client) {
		if (msBusy) return;
		msBusy = true;
		msStatus = "Contacting Microsoft...";
		msCode = "";
		mode = Mode.MICROSOFT;
		MicrosoftAuth.startAsync(new MicrosoftAuth.Listener() {
			@Override
			public void onStatus(final String message, final String userCode) {
				client.execute(new java.util.concurrent.Callable<Object>() {
					@Override
					public Object call() {
						String code = userCode == null ? "" : userCode;
						if (!code.isEmpty() && !code.equals(msCode)) {
							msCode = code;
							Screen.setClipboard(code);
							msStatus = "Code copied. Paste it in the browser.";
						} else {
							msStatus = message;
						}
						return null;
					}
				});
			}

			@Override
			public void onSuccess(final SharedAccount account) {
				client.execute(new java.util.concurrent.Callable<Object>() {
					@Override
					public Object call() {
						msBusy = false;
						SessionSwap.applyNew(client, account);
						reload();
						mode = Mode.CLOSED;
						toast("Signed in as " + account.username);
						return null;
					}
				});
			}

			@Override
			public void onError(final String message) {
				client.execute(new java.util.concurrent.Callable<Object>() {
					@Override
					public Object call() {
						msBusy = false;
						msStatus = message;
						toast(message);
						mode = Mode.SIGN_IN;
						return null;
					}
				});
			}
		});
	}

	private void toast(String message) {
		status = message;
		statusUntil = System.currentTimeMillis() + 3500L;
	}

	private SharedAccount resolveActive(MinecraftClient client) {
		SharedAccount active = snap.active();
		if (active != null) return active;
		Session session = client.getSession();
		return new SharedAccount(session.getUuid(), session.getUsername(), session.getUuid(), "session", session.getAccessToken(), "");
	}

	private List<SharedAccount> others(SharedAccount active) {
		List<SharedAccount> list = new ArrayList<SharedAccount>();
		String activeId = active == null ? "" : active.id;
		String activeName = active == null ? "" : active.username;
		for (SharedAccount account : snap.accounts) {
			if (account.id.equals(activeId) || account.username.equalsIgnoreCase(activeName)) continue;
			list.add(account);
		}
		return list;
	}

	private static String displayName(MinecraftClient client, SharedAccount active) {
		String name = active != null ? active.username : client.getSession().getUsername();
		return name == null ? "" : name.toUpperCase();
	}

	/** Dropdown opens under the chip, aligned to its left edge. */
	private static int menuX(int chipX, int chipW, int menuW) {
		return Math.max(4, chipX);
	}

	private static String fitText(MinecraftClient client, String text, int maxW, float size) {
		if (text == null) return "";
		if (OpticUiFont.widthSharp(client, text, 0xFFFFFFFF, size) <= maxW) return text;
		String ellipsis = "...";
		String base = text;
		while (base.length() > 1 && OpticUiFont.widthSharp(client, base + ellipsis, 0xFFFFFFFF, size) > maxW) {
			base = base.substring(0, base.length() - 1);
		}
		return base + ellipsis;
	}

	private static void drawBadge(MinecraftClient client, int x, int y, int size) {
		OpticUiDraw.fillExact(client, x, y, size, size, size / 2, 0xFF101014);
		Identifier logo = OpticTextures.logo(client);
		if (logo == null) return;
		int mark = Math.max(4, size - 2);
		int ox = x + (size - mark) / 2;
		int oy = y + (size - mark) / 2;
		gg.optic.client.OpticSmooth.bindMip(client, logo);
		int tw = Math.max(1, OpticTextures.logoWidth());
		int th = Math.max(1, OpticTextures.logoHeight());
		DrawableHelper.drawTexture(ox, oy, 0.0F, 0.0F, tw, th, mark, mark, (float) tw, (float) th);
	}

	private static void drawIcon(MinecraftClient client, Identifier icon, int x, int y, int size, int argb, boolean flipY) {
		if (icon == null || size <= 0) return;
		float a = ((argb >> 24) & 0xFF) / 255.0F;
		float r = ((argb >> 16) & 0xFF) / 255.0F;
		float g = ((argb >> 8) & 0xFF) / 255.0F;
		float b = (argb & 0xFF) / 255.0F;
		gg.optic.client.OpticSmooth.bind(client, icon);
		GlStateManager.color(r, g, b, a);
		if (flipY) {
			DrawableHelper.drawTexture(x, y, 0.0F, 128.0F, 128, -128, size, size, 128.0F, 128.0F);
		} else {
			DrawableHelper.drawTexture(x, y, 0.0F, 0.0F, 128, 128, size, size, 128.0F, 128.0F);
		}
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static Identifier chevron(MinecraftClient client) {
		if (chevronIcon != null) return chevronIcon;
		try {
			int size = 128;
			BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setColor(Color.WHITE);
			g.setStroke(new BasicStroke(size / 9.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int left = size / 5;
			int right = size - left;
			int top = size / 3;
			int bottom = size - size / 4;
			g.drawLine(left, top, size / 2, bottom);
			g.drawLine(size / 2, bottom, right, top);
			g.dispose();
			NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
			chevronIcon = client.getTextureManager().registerDynamicTexture("optic_account_chevron", texture);
			gg.optic.client.OpticSmooth.linearize(texture);
		} catch (Exception e) {
			System.err.println("[Optic] Chevron icon failed: " + e.getMessage());
		}
		return chevronIcon;
	}

	private static Identifier removeIcon(MinecraftClient client) {
		if (removeMark != null) return removeMark;
		try {
			int size = 128;
			BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setColor(Color.WHITE);
			g.setStroke(new BasicStroke(size / 12.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			int pad = size / 4;
			g.drawLine(pad, pad, size - pad, size - pad);
			g.drawLine(size - pad, pad, pad, size - pad);
			g.dispose();
			NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
			removeMark = client.getTextureManager().registerDynamicTexture("optic_account_remove", texture);
			gg.optic.client.OpticSmooth.linearize(texture);
		} catch (Exception e) {
			System.err.println("[Optic] Remove icon failed: " + e.getMessage());
		}
		return removeMark;
	}

	private static void drawHead(MinecraftClient client, String uuid, int x, int y, int size) {
		drawHead(client, uuid, null, x, y, size);
	}

	/** The account's real skin face; nearest filtering keeps the skin pixels square. */
	private static void drawHead(MinecraftClient client, String uuid, String name, int x, int y, int size) {
		Identifier head = AccountHeads.get(client, uuid, name);
		if (head != null) {
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.enableBlend();
			GlStateManager.blendFunc(770, 771);
			client.getTextureManager().bindTexture(head);
			net.minecraft.client.texture.Texture bound = client.getTextureManager().getTexture(head);
			if (bound instanceof net.minecraft.client.texture.AbstractTexture) {
				gg.optic.client.compat.Mc17.setFilter((net.minecraft.client.texture.AbstractTexture) bound, false, false);
			}
			DrawableHelper.drawTexture(x, y, 0.0F, 0.0F, 128, 128, size, size, 128.0F, 128.0F);
			GlStateManager.disableBlend();
		} else {
			fill(x, y, x + size, y + size, 0xFF2A2A32);
		}
	}

	private static boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	private static void fill(int x1, int y1, int x2, int y2, int argb) {
		DrawableHelper.fill(x1, y1, x2, y2, argb);
	}
}
