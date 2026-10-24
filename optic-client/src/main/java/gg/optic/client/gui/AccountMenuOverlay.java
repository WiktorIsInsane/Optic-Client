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
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.Texture;
import net.minecraft.client.util.Session;
import net.minecraft.util.Identifier;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Top-right account chip + dropdown (hot-swap, sign-in offline/Microsoft). */
public final class AccountMenuOverlay {
	private static final int ACCENT = 0xFF5CFF8A;
	private static final int ACCENT_HOVER = 0xFF3DB86A;
	private static final int BACK = 0xFFFF4D4D;
	private static final int CHIP = OpticMenuChrome.GLASS_20;
	private static final int CHIP_HOT = OpticMenuChrome.GLASS_20_HOT;
	private static final int PANEL = 0xF0101014;
	private static final int HEAD = 14;
	private static final int CHEV = 7;
	private static final float NAME_SIZE = 8.0F;
	private static final int CHIP_H = OpticMenuChrome.BAR_H;
	private static final int CHIP_Y = OpticMenuChrome.BAR_Y;
	private static Identifier chevronIcon;
	private static Identifier removeMark;
	private static final int ROW_H = 20;
	private static final int ROW_HEAD = 12;
	private static final int ROW_X = 10;
	private static final int MENU_MIN_W = 168;
	private static final float ROW_SIZE = 8.0F;
	/** Room for the close button plus the gap between the two boxes. */
	private static final int RIGHT_GUTTER = OpticMenuChrome.BAR_EDGE + OpticMenuChrome.BAR_H + OpticMenuChrome.BAR_GAP;

	private enum Mode {
		CLOSED,
		LIST,
		SIGN_IN,
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

	public void reload() {
		snap = SharedAccounts.load();
		lastReloadMs = System.currentTimeMillis();
	}

	public int chipWidth(MinecraftClient client) {
		SharedAccount active = resolveActive(client);
		String name = displayName(client, active);
		int nameW = OpticUiFont.widthSharp(client, name, 0xFFFFFFFF, NAME_SIZE);
		return Math.min(220, 4 + HEAD + 6 + nameW + 8 + CHEV + 8);
	}

	public int chipX(MinecraftClient client, int screenWidth) {
		return Math.max(4, screenWidth - RIGHT_GUTTER - chipWidth(client));
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

		OpticUiDraw.fillExact(client, x, y, chipW, CHIP_H, CHIP_H / 2, hot ? CHIP_HOT : CHIP);
		int headX = x + 3;
		int headY = y + (CHIP_H - HEAD) / 2;
		drawHead(client, uuid, headX, headY, HEAD);
		drawBadge(client, headX + HEAD - 6, headY + HEAD - 6, 8);
		int nameH = OpticUiFont.heightSharp(client, name, 0xFFFFFFFF, NAME_SIZE);
		OpticUiFont.drawSharp(client, name, headX + HEAD + 6, y + (CHIP_H - nameH) / 2, 0xFFFFFFFF, NAME_SIZE);
		drawIcon(client, chevron(client), x + chipW - 8 - CHEV, y + (CHIP_H - CHEV) / 2, CHEV, 0xFFE4E4EA, mode != Mode.CLOSED);

		if (mode == Mode.LIST) {
			List<SharedAccount> others = others(active);
			int menuW = menuW(chipW);
			int menuH = listHeight(others.size());
			int menuX = menuX(x, chipW, menuW);
			int menuY = y + CHIP_H + 4;
			OpticUiDraw.fillExact(client, menuX, menuY, menuW, menuH, 8, PANEL);

			int rowY = menuY + 4;
			for (SharedAccount account : others) {
				boolean rowHot = inside(mouseX, mouseY, menuX, rowY, menuW, ROW_H);
				if (rowHot) OpticUiDraw.fillExact(client, menuX + 3, rowY, menuW - 6, ROW_H, 4, 0x22FFFFFF);
				drawHead(client, account.uuid, menuX + 6, rowY + (ROW_H - ROW_HEAD) / 2, ROW_HEAD);
				int nameMax = menuW - 6 - ROW_HEAD - 6 - ROW_X - 10;
				String ign = fitText(client, account.username == null ? "" : account.username, nameMax, ROW_SIZE);
				int ignH = OpticUiFont.heightSharp(client, ign, 0xFFFFFFFF, ROW_SIZE);
				OpticUiFont.drawSharp(client, ign, menuX + 6 + ROW_HEAD + 6, rowY + (ROW_H - ignH) / 2, 0xFFFFFFFF, ROW_SIZE);
				int xBtn = menuX + menuW - 6 - ROW_X;
				int xBtnY = rowY + (ROW_H - ROW_X) / 2;
				boolean xHot = inside(mouseX, mouseY, xBtn - 2, xBtnY - 2, ROW_X + 4, ROW_X + 4);
				drawIcon(client, removeIcon(client), xBtn, xBtnY, ROW_X, xHot ? 0xFFFFFFFF : 0xFF9A9AA8, false);
				rowY += ROW_H;
			}
			String add = "Add account";
			boolean addHot = inside(mouseX, mouseY, menuX, rowY, menuW, ROW_H);
			int addColor = addHot ? ACCENT_HOVER : ACCENT;
			int addW = OpticUiFont.widthSharp(client, add, addColor, ROW_SIZE);
			int addH = OpticUiFont.heightSharp(client, add, addColor, ROW_SIZE);
			OpticUiFont.drawSharp(client, add, menuX + (menuW - addW) / 2, rowY + (ROW_H - addH) / 2, addColor, ROW_SIZE);
		} else if (mode == Mode.SIGN_IN) {
			int menuW = menuW(chipW);
			int menuH = 8 + ROW_H * 3;
			int menuX = menuX(x, chipW, menuW);
			int menuY = y + CHIP_H + 4;
			OpticUiDraw.fillExact(client, menuX, menuY, menuW, menuH, 8, PANEL);
			int line = menuY + 4;
			OpticUiFont.drawSharp(client, "Back", menuX + 8, line + 4, BACK, ROW_SIZE);
			OpticUiFont.drawSharp(client, "Offline account", menuX + 8, line + ROW_H + 4, 0xFFFFFFFF, ROW_SIZE);
			OpticUiFont.drawSharp(client, "Microsoft account", menuX + 8, line + ROW_H * 2 + 4, ACCENT, ROW_SIZE);
		} else if (mode == Mode.MICROSOFT) {
			int menuW = menuW(chipW);
			int menuH = 52;
			int menuX = menuX(x, chipW, menuW);
			int menuY = y + CHIP_H + 4;
			OpticUiDraw.fillExact(client, menuX, menuY, menuW, menuH, 8, PANEL);
			OpticUiFont.drawSharp(client, "Microsoft sign-in", menuX + 8, menuY + 8, 0xFFE8E8E8, 9.0F);
			OpticUiFont.drawSharp(client, msStatus.isEmpty() ? "Starting..." : msStatus, menuX + 8, menuY + 24, 0xFFAAAAAA, 9.0F);
			OpticUiFont.drawSharp(client, msBusy ? "Waiting in browser..." : "Click to cancel", menuX + 8, menuY + 38, 0xFF777777, 9.0F);
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

		if (inside(mouseX, mouseY, x, y, chipW, CHIP_H)) {
			mode = mode == Mode.CLOSED ? Mode.LIST : Mode.CLOSED;
			return true;
		}

		if (mode == Mode.LIST) {
			List<SharedAccount> others = others(active);
			int menuW = menuW(chipW);
			int menuH = listHeight(others.size());
			int menuX = menuX(x, chipW, menuW);
			int menuY = y + CHIP_H + 4;
			int rowY = menuY + 4;
			for (SharedAccount account : others) {
				int xBtn = menuX + menuW - 6 - ROW_X;
				int xBtnY = rowY + (ROW_H - ROW_X) / 2;
				if (inside(mouseX, mouseY, xBtn - 2, xBtnY - 2, ROW_X + 4, ROW_X + 4)) {
					SharedAccounts.remove(account.id);
					reload();
					return true;
				}
				if (inside(mouseX, mouseY, menuX, rowY, menuW, ROW_H)) {
					SessionSwap.apply(client, account);
					reload();
					mode = Mode.CLOSED;
					toast("Signed in as " + account.username);
					return true;
				}
				rowY += ROW_H;
			}
			if (inside(mouseX, mouseY, menuX, rowY, menuW, ROW_H)) {
				mode = Mode.SIGN_IN;
				return true;
			}
			if (!inside(mouseX, mouseY, menuX, menuY, menuW, menuH)) {
				mode = Mode.CLOSED;
			}
			return true;
		}

		if (mode == Mode.SIGN_IN) {
			int menuW = menuW(chipW);
			int menuH = 8 + ROW_H * 3;
			int menuX = menuX(x, chipW, menuW);
			int menuY = y + CHIP_H + 4;
			int backY = menuY + 4;
			int offlineY = backY + ROW_H;
			int msY = offlineY + ROW_H;
			if (inside(mouseX, mouseY, menuX, backY, menuW, ROW_H)) {
				mode = Mode.LIST;
				return true;
			}
			if (inside(mouseX, mouseY, menuX, offlineY, menuW, ROW_H)) {
				mode = Mode.CLOSED;
				client.setScreen(new OfflineSignInScreen(client.currentScreen instanceof TitleScreen
					? client.currentScreen
					: new TitleScreen()));
				return true;
			}
			if (inside(mouseX, mouseY, menuX, msY, menuW, ROW_H)) {
				startMicrosoft(client);
				return true;
			}
			if (!inside(mouseX, mouseY, menuX, menuY, menuW, menuH)) {
				mode = Mode.CLOSED;
			}
			return true;
		}

		if (mode == Mode.MICROSOFT) {
			if (!msBusy) {
				mode = Mode.LIST;
			}
			return true;
		}

		return false;
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

	private static int menuW(int chipW) {
		return Math.max(MENU_MIN_W, chipW);
	}

	private static int listHeight(int otherCount) {
		return 4 + otherCount * ROW_H + ROW_H + 4;
	}

	private static int menuX(int chipX, int chipW, int menuW) {
		return Math.max(4, chipX + chipW - menuW);
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
		Identifier head = AccountHeads.get(client, uuid);
		if (head != null) {
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.enableBlend();
			GlStateManager.blendFunc(770, 771);
			client.getTextureManager().bindTexture(head);
			net.minecraft.client.texture.Texture bound = client.getTextureManager().getTexture(head);
			if (bound instanceof net.minecraft.client.texture.AbstractTexture) {
				((net.minecraft.client.texture.AbstractTexture) bound).setFilter(true, true);
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
