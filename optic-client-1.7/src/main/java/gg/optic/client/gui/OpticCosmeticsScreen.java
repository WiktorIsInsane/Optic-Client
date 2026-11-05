package gg.optic.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticFrame;
import gg.optic.client.OpticUiDraw;
import gg.optic.client.OpticUiFont;
import gg.optic.client.account.AccountHeads;
import gg.optic.client.account.SharedAccount;
import gg.optic.client.account.SharedAccounts;
import gg.optic.client.cosmetics.OpticCosmetics;
import gg.optic.client.cosmetics.SharedCosmetics;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.entity.model.BiPedModel;
import net.minecraft.client.render.model.ModelPart;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Cosmetics menu: logo + title, four text tabs on the left, items in the middle and a
 * player preview on the right. No panels or fills: everything sits straight on the game
 * (or the menu wallpaper). Equipping writes to the file the launcher reads, and launcher
 * changes show up live.
 */
public final class OpticCosmeticsScreen extends Screen {
	private static final float TITLE_SIZE = 12.0F;
	private static final float TEXT_SIZE = 9.5F;
	private static final float SMALL_SIZE = 8.0F;
	private static final int LOGO = 16;
	private static final int TAB_H = 18;
	private static final int ROW_H = 20;
	private static final int SIDE_W = 92;
	private static final int PREVIEW_W = 130;
	private static final int HOVER = 0x1CFFFFFF;
	private static final int RULE = 0x33FFFFFF;
	/** Brighter than the theme's muted grey so idle text still reads over the game. */
	private static final int IDLE = 0xFFC9CED8;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int SHADOW = 0x99000000;

	private static final int TILE_H = 76;
	private static final int TILE_MIN_W = 66;
	private static final int TILE = 0x0FFFFFFF;

	private static int tab;
	private static float yaw = 25.0F;
	/** Middle-click on the preview: head faces straight ahead and ignores the mouse. */
	private static boolean headLocked;
	/** Item under the cursor this frame ("" = None), or null. Tried on in the preview. */
	private String hoverId;

	private final Screen parent;
	private final BiPedModel model = new BiPedModel(0.0F);
	private int boxX, boxY, boxW, boxH;
	private int listX, listY, listW, listH;
	private int previewX, previewW;
	private int closeX, closeY;
	private int scroll;
	private boolean dragging;
	private int dragLastX;
	private String skinUuid;
	private String skinName;
	private final List<int[]> rowHits = new ArrayList<int[]>();
	private final List<String> rowIds = new ArrayList<String>();

	public OpticCosmeticsScreen(Screen parent) {
		this.parent = parent;
	}

	@Override
	public void init() {
		buttons.clear();
		boxW = Math.min(540, Math.max(1, width - 32));
		boxH = Math.min(260, Math.max(1, height - 32));
		boxX = (width - boxW) / 2;
		boxY = (height - boxH) / 2;
		closeX = boxX + boxW - 14;
		closeY = boxY + 3;
		// Narrow windows drop the preview rather than squeeze the list.
		previewW = boxW >= 360 ? PREVIEW_W : 0;
		previewX = boxX + boxW - previewW;
		listX = boxX + SIDE_W + 12;
		listY = boxY + 36;
		listW = (previewW > 0 ? previewX - 12 : boxX + boxW) - listX;
		listH = boxY + boxH - listY;
		SharedCosmetics.refresh();
		resolveSkinOwner();
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		if (dragging) {
			if (!Mouse.isButtonDown(0)) {
				dragging = false;
			} else {
				yaw += (mouseX - dragLastX) * 2.0F;
				dragLastX = mouseX;
			}
		}
		// Fully transparent in game. On the title screen there is no world, so keep its wallpaper.
		if (client.world == null) OpticMenuChrome.drawWallpaper(client, width, height);
		OpticFrame.begin(client);
		try {
			drawHeader(mouseX, mouseY);
			drawTabs(mouseX, mouseY);
			drawList(mouseX, mouseY);
		} finally {
			OpticFrame.end();
		}
		if (previewW > 0) drawPreview(mouseX, mouseY, delta);
	}

	private void drawHeader(int mouseX, int mouseY) {
		OpticMenuChrome.drawLogo(client, boxX, boxY, LOGO);
		int th = OpticUiFont.heightSharp(client, "Cosmetics", WHITE, TITLE_SIZE);
		text("Cosmetics", boxX + LOGO + 7, boxY + (LOGO - th) / 2, WHITE, TITLE_SIZE);

		boolean closeHot = inside(mouseX, mouseY, closeX - 3, closeY - 3, 18, 18);
		OpticUiFont.drawCenteredSized(client, "×", closeX + 7, closeY - 2, SHADOW, 14.0F);
		OpticUiFont.drawCenteredSized(client, "×", closeX + 6, closeY - 3, closeHot ? OpticTheme.ACCENT : WHITE, 14.0F);

		DrawableHelper.fill(boxX, boxY + 25, boxX + boxW, boxY + 26, RULE);
	}

	private void drawTabs(int mouseX, int mouseY) {
		int y = listY;
		for (int i = 0; i < SharedCosmetics.CATEGORIES.length; i++) {
			boolean active = i == tab;
			boolean hot = inside(mouseX, mouseY, boxX, y, SIDE_W, TAB_H);
			if (active) DrawableHelper.fill(boxX, y + 4, boxX + 2, y + TAB_H - 4, OpticTheme.ACCENT);
			int color = active || hot ? WHITE : IDLE;
			String label = SharedCosmetics.LABELS[i];
			int th = OpticUiFont.heightSharp(client, label, color, TEXT_SIZE);
			text(label, boxX + 9, y + (TAB_H - th) / 2, color, TEXT_SIZE);
			if (!SharedCosmetics.equipped(SharedCosmetics.CATEGORIES[i]).isEmpty()) {
				DrawableHelper.fill(boxX + SIDE_W - 6, y + TAB_H / 2 - 1, boxX + SIDE_W - 3, y + TAB_H / 2 + 2, OpticTheme.ACCENT);
			}
			y += TAB_H + 2;
		}
		DrawableHelper.fill(boxX + SIDE_W + 4, listY, boxX + SIDE_W + 5, boxY + boxH, RULE);
		if (previewW > 0) DrawableHelper.fill(previewX - 6, listY, previewX - 5, boxY + boxH, RULE);
	}

	private void drawList(int mouseX, int mouseY) {
		rowHits.clear();
		rowIds.clear();
		String category = SharedCosmetics.CATEGORIES[tab];
		List<SharedCosmetics.Item> items = SharedCosmetics.items(category);
		String on = SharedCosmetics.equipped(category);

		int gap = 6;
		int cols = Math.max(2, (listW + gap) / (TILE_MIN_W + gap));
		int cellW = (listW - gap * (cols - 1)) / cols;
		int total = items.size() + 1;
		int rows = (total + cols - 1) / cols;
		int maxScroll = Math.max(0, rows * (TILE_H + gap) - listH);
		if (scroll > maxScroll) scroll = maxScroll;
		if (scroll < 0) scroll = 0;

		hoverId = null;
		boolean depthCleared = false;
		clip(listX, listY, listW, listH);
		try {
			for (int i = 0; i < total; i++) {
				String id = i == 0 ? "" : items.get(i - 1).id;
				String name = i == 0 ? "None" : items.get(i - 1).name;
				int x = listX + (i % cols) * (cellW + gap);
				int y = listY + (i / cols) * (TILE_H + gap) - scroll;
				if (y + TILE_H < listY || y > listY + listH) continue;
				boolean equipped = id.equals(on);
				boolean hot = inside(mouseX, mouseY, x, y, cellW, TILE_H) && mouseY >= listY && mouseY < listY + listH;
				if (hot) hoverId = id;
				OpticUiDraw.fillExact(client, x, y, cellW, TILE_H, OpticTheme.RADIUS, hot ? HOVER : TILE);
				if (equipped) OpticUiDraw.outlineExact(client, x, y, cellW, TILE_H, OpticTheme.RADIUS, OpticTheme.ACCENT);

				int color = equipped ? OpticTheme.ACCENT : (hot ? WHITE : IDLE);
				String label = OpticUiFont.widthSharp(client, name, color, SMALL_SIZE) > cellW - 8 ? shorten(name, cellW - 8) : name;
				int lw = OpticUiFont.widthSharp(client, label, color, SMALL_SIZE);
				// "None" has nothing to show, so its name sits in the middle of the tile.
				int labelY = id.isEmpty() ? y + (TILE_H - 8) / 2 : y + TILE_H - 13;
				text(label, x + (cellW - lw) / 2, labelY, color, SMALL_SIZE);

				int artH = TILE_H - 18;
				if (!id.isEmpty()) {
					boolean cape = OpticCosmetics.CAPE_OPTIC.equals(id);
					boolean wings = OpticCosmetics.WINGS_DRAGON.equals(id);
					// Small model seen from behind, wearing just this item, turning slowly.
					float size = artH / (wings ? 2.7F : 2.3F);
					float neckY = y + 4 + (artH - size * 2.0F) / 2.0F + size * 0.5F;
					float spin = 160.0F + (float) Math.sin(System.currentTimeMillis() / 900.0) * 25.0F;
					drawModel(x + cellW / 2.0F, neckY, size, spin, spin, 0.0F, cape, wings, !depthCleared);
					depthCleared = true;
				}
				rowHits.add(new int[] { x, y, cellW, TILE_H });
				rowIds.add(id);
			}
			if (items.isEmpty()) {
				text("Nothing here yet", listX + 2, listY + TILE_H + gap + 6, IDLE, SMALL_SIZE);
			}
		} finally {
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
		}
	}

	private String shorten(String value, int maxW) {
		String s = value;
		while (s.length() > 1 && OpticUiFont.widthSharp(client, s + "…", IDLE, SMALL_SIZE) > maxW) {
			s = s.substring(0, s.length() - 1);
		}
		return s + "…";
	}

	/**
	 * The player's model with their own skin, slowly idling. Drag to turn it, middle-click to
	 * make it look straight ahead. Hovering an item tries it on here.
	 */
	private void drawPreview(int mouseX, int mouseY, float delta) {
		String category = SharedCosmetics.CATEGORIES[tab];
		boolean cape = OpticCosmetics.capeEquipped();
		boolean wings = OpticCosmetics.wingsEquipped();
		if (hoverId != null && "capes".equals(category)) cape = OpticCosmetics.CAPE_OPTIC.equals(hoverId);
		if (hoverId != null && "wings".equals(category)) wings = OpticCosmetics.WINGS_DRAGON.equals(hoverId);

		int areaY = listY;
		int areaH = boxY + boxH - areaY;
		float size = Math.min(previewW * 0.42F, areaH / 2.3F);
		// Wings reach well past the shoulders; shrink the model so they stay in the column.
		if (wings) size *= 0.88F;
		float cx = previewX + previewW / 2.0F;
		// Model space: y=0 is the neck, head goes up 0.5, feet are 1.5 down (units of size).
		float neckY = areaY + (areaH - size * 2.0F) / 2.0F + size * 0.5F;

		float bodyYaw = wrap(yaw);
		float headYaw = bodyYaw;
		float headPitch = 0.0F;
		if (!headLocked) {
			// Head turns toward the cursor, worked out on screen (positive yaw = faces right,
			// positive pitch = looks up) and kept within 70 degrees of where the body faces.
			float reach = Math.max(1.0F, size * 2.5F);
			headYaw = (float) Math.toDegrees(Math.atan2(mouseX - cx, reach));
			headYaw = clamp(headYaw, bodyYaw - 70.0F, bodyYaw + 70.0F);
			headPitch = (float) Math.toDegrees(Math.atan2((neckY - size * 0.25F) - mouseY, reach));
			headPitch = clamp(headPitch, -35.0F, 35.0F);
		}
		drawModel(cx, neckY, size, bodyYaw, headYaw, headPitch, cape, wings, true);
	}

	/**
	 * One player model at (cx, neckY), {@code size} GUI px per block. Yaws are in screen terms:
	 * 0 faces the viewer, positive turns toward the right.
	 */
	private void drawModel(float cx, float neckY, float size, float bodyYaw, float headYaw, float headPitch,
			boolean cape, boolean wings, boolean clearDepth) {
		float t = (System.currentTimeMillis() % 100000L) / 50.0F;
		resetPose();
		model.field_1476.posZ = (float) Math.cos(t * 0.09F) * 0.05F + 0.05F;
		model.field_1477.posZ = -((float) Math.cos(t * 0.09F) * 0.05F + 0.05F);
		model.field_1476.posX = (float) Math.sin(t * 0.067F) * 0.05F;
		model.field_1477.posX = -(float) Math.sin(t * 0.067F) * 0.05F;

		client.getTextureManager().bindTexture(skin());
		// Put alpha test back how the caller had it: the GUI text pass wants it off.
		boolean alphaWasOn = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
		if (clearDepth) GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
		GlStateManager.enableDepthTest();
		GlStateManager.depthMask(true);
		GlStateManager.enableAlphaTest();
		GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
		GlStateManager.enableColorMaterial();
		GlStateManager.enableRescaleNormal();
		GlStateManager.disableBlend();
		GlStateManager.disableCull();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.pushMatrix();
		try {
			GlStateManager.translate(cx, neckY, 200.0F);
			// Lights fixed to the view, so turning the model doesn't move its shading.
			GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
			DiffuseLighting.enable();
			GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
			// GUI space has y flipped, so mirror x back or the model renders left/right swapped.
			GlStateManager.scale(-size, size, size);
			// Slight top-down view, like looking at the player from just above head height.
			GlStateManager.rotate(-8.0F, 1.0F, 0.0F, 0.0F);
			float s = 0.0625F;
			// Head pivots at the neck (the origin here), in screen space so it really faces the cursor.
			GlStateManager.pushMatrix();
			GlStateManager.rotate(headPitch, 1.0F, 0.0F, 0.0F);
			GlStateManager.rotate(180.0F - headYaw, 0.0F, 1.0F, 0.0F);
			model.head.render(s);
			model.hat.render(s);
			GlStateManager.popMatrix();
			GlStateManager.rotate(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);
			model.body.render(s);
			model.field_1476.render(s);
			model.field_1477.render(s);
			model.field_1478.render(s);
			model.field_1479.render(s);
			if (cape) OpticCosmetics.drawCape(8.0F + (float) Math.sin(t * 0.05F) * 3.0F, 0.0F);
			if (wings) OpticCosmetics.renderWings(false);
			GlStateManager.disableCull();
		} finally {
			DiffuseLighting.disable();
			GlStateManager.popMatrix();
			GlStateManager.disableRescaleNormal();
			GlStateManager.disableColorMaterial();
			GlStateManager.disableDepthTest();
			GlStateManager.enableBlend();
			if (!alphaWasOn) GlStateManager.disableAlphaTest();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	private void resetPose() {
		ModelPart[] parts = { model.head, model.hat, model.body, model.field_1476, model.field_1477, model.field_1478, model.field_1479 };
		for (ModelPart part : parts) {
			part.posX = 0.0F;
			part.posY = 0.0F;
			part.posZ = 0.0F;
		}
	}

	/** The skin that belongs to the player's username (looked up online), Steve until it loads. */
	private Identifier skin() {
		Identifier id = AccountHeads.skin(client, skinUuid, skinName);
		return id != null ? id : AbstractClientPlayerEntity.STEVE_TEXTURE;
	}

	/** In game: the logged-in name. On the title screen: the launcher's active account. */
	private void resolveSkinOwner() {
		skinUuid = client.getSession().getUuid();
		skinName = client.getSession().getUsername();
		if (client.world != null) return;
		SharedAccounts.Snapshot snap = SharedAccounts.load();
		for (SharedAccount account : snap.accounts) {
			if (account.id.equals(snap.activeAccountId)) {
				skinUuid = account.uuid;
				skinName = account.username;
			}
		}
	}

	/** Text with a soft drop shadow so it reads over any background. */
	private void text(String value, int x, int y, int color, float size) {
		OpticUiFont.drawSharp(client, value, x + 1, y + 1, SHADOW, size);
		OpticUiFont.drawSharp(client, value, x, y, color, size);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (button == 2 && previewW > 0 && inside(mouseX, mouseY, previewX, listY, previewW, boxY + boxH - listY)) {
			// Toggle: look straight ahead (body squared up too) / follow the mouse again.
			headLocked = !headLocked;
			if (headLocked) yaw = 0.0F;
			return;
		}
		if (button != 0) return;
		if (inside(mouseX, mouseY, closeX - 3, closeY - 3, 18, 18)) {
			close();
			return;
		}
		if (previewW > 0 && inside(mouseX, mouseY, previewX, listY, previewW, boxY + boxH - listY)) {
			dragging = true;
			dragLastX = mouseX;
			return;
		}
		int y = listY;
		for (int i = 0; i < SharedCosmetics.CATEGORIES.length; i++) {
			if (inside(mouseX, mouseY, boxX, y, SIDE_W, TAB_H)) {
				if (tab != i) {
					tab = i;
					scroll = 0;
				}
				return;
			}
			y += TAB_H + 2;
		}
		if (mouseY < listY || mouseY >= listY + listH) return;
		for (int i = 0; i < rowHits.size(); i++) {
			int[] r = rowHits.get(i);
			if (inside(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
				SharedCosmetics.equip(SharedCosmetics.CATEGORIES[tab], rowIds.get(i));
				return;
			}
		}
	}

	@Override
	public void handleMouse() {
		super.handleMouse();
		int wheel = Mouse.getEventDWheel();
		if (wheel != 0) scroll += wheel > 0 ? -(TILE_H / 2) : TILE_H / 2;
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			close();
			return;
		}
		super.keyPressed(character, keyCode);
	}

	private void close() {
		client.setScreen(parent);
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}

	private void clip(int x, int y, int w, int h) {
		int sf = Math.max(1, new Window(client, client.width, client.height).getScaleFactor());
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(x * sf, client.height - (y + h) * sf, Math.max(0, w) * sf, Math.max(0, h) * sf);
	}

	private static float wrap(float degrees) {
		float d = degrees % 360.0F;
		if (d >= 180.0F) d -= 360.0F;
		if (d < -180.0F) d += 360.0F;
		return d;
	}

	private static float clamp(float v, float lo, float hi) {
		return v < lo ? lo : (v > hi ? hi : v);
	}

	private static boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}
}
