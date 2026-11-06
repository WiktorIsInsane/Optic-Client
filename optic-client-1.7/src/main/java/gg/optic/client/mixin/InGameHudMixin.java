package gg.optic.client.mixin;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticGui;
import gg.optic.client.OpticHudLayout;
import gg.optic.client.OpticUiFont;
import gg.optic.client.hud.OpticCrosshair;
import gg.optic.client.hud.OpticHud;
import gg.optic.client.hud.OpticKeystrokes;
import gg.optic.client.hud.OpticPotions;
import gg.optic.client.hud.OpticTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

/** Draws Optic's HUD modules on top of the 1.7.10 in-game HUD. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin extends DrawableHelper {
	@Unique private static final Identifier OPTIC_WIDGETS = new Identifier("textures/gui/widgets.png");

	@Shadow @Final private MinecraftClient client;

	/** 1.7 yarn calls the hotbar slot renderer renderActionBar(slot, x, y, tickDelta). */
	@Shadow
	protected abstract void renderActionBar(int slot, int x, int y, float tickDelta);

	@Inject(method = "render(FZII)V", at = @At("TAIL"))
	private void optic$render(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
		optic$endHotbarTransform(); // never leak the status-bar transform past the HUD
		if (client.options.debugEnabled || client.options.hudHidden) return;
		// The live HUD stays visible under the mod menu, where it can be edited too.
		OpticConfig config = OpticClient.get().config();
		boolean freezeInput = OpticGui.isOpticScreenOpen();
		Window window = new Window(client, client.width, client.height);
		// 1.7 leaves its last HUD draw's state behind (no GlStateManager to reset it).
		GlStateManager.enableTexture();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GlStateManager.enableAlphaTest();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		if (config.hotbar) optic$drawHotbar(config, window, tickDelta);
		if (config.fps) OpticHud.drawFps(client, config);
		if (config.cps) OpticHud.drawCps(client, config);
		if (config.ping) OpticHud.drawPing(client, config);
		if (config.coordinates) OpticHud.drawCoordinates(client, config);
		OpticPotions.render(client);
		OpticCrosshair.render(client, window.getWidth() / 2, window.getHeight() / 2);
		if (config.toggleSprint && config.toggleSprintShowHud) OpticHud.drawSprint(client, config);
		if (config.keystrokes) OpticKeystrokes.render(client, config, freezeInput);
		// Pearl cooldown and graphs; shown even when idle while the HUD can be edited.
		boolean editingHud = client.currentScreen instanceof gg.optic.client.gui.OpticHudEditorScreen
			|| client.currentScreen instanceof gg.optic.client.gui.OpticModScreen;
		gg.optic.client.hud.OpticWidgets.renderAll(client, config, editingHud);
		if (OpticTab.visible(client) && client.world != null) {
			OpticTab.render(client, window.getWidth(), client.world.getScoreboard(), client.world.getScoreboard().getObjectiveForSlot(0));
		}
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/**
	 * 1.7 draws its own tab list inline in render (a fixed grid sized to the server's max
	 * players). Always hide it: OpticTab draws the 1.8.9-style list instead.
	 */
	@Redirect(
		method = "render(FZII)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z")
	)
	private boolean optic$vanillaTab(KeyBinding binding) {
		return false;
	}

	/**
	 * 1.7 draws the crosshair (16x16 at 0,0) and the hotbar (182x22 bar, 24x22 selection)
	 * inline in render. Skip them when Optic's crosshair or Hotbar module draws its own.
	 */
	@Redirect(
		method = "render(FZII)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V")
	)
	private void optic$vanillaWidgets(InGameHud self, int x, int y, int u, int v, int width, int height) {
		OpticConfig c = OpticClient.get().config();
		boolean crosshair = u == 0 && v == 0 && width == 16 && height == 16;
		if (crosshair && OpticCrosshair.hidesVanilla(c)) return;
		boolean hotbar = u == 0 && ((v == 0 && width == 182 && height == 22) || (v == 22 && width == 24 && height == 22));
		if (hotbar && c.hotbar) return;
		// XP bar (v 64/69) and horse jump bar (v 84/89), 182 wide: the Hotbar module's XP toggle.
		boolean xpBar = u == 0 && width == 182 && (v == 64 || v == 69 || v == 84 || v == 89);
		if (xpBar && c.hotbar && !c.hotbarShowXp) return;
		self.drawTexture(x, y, u, v, width, height);
	}

	@Redirect(
		method = "render(FZII)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderActionBar(IIIF)V")
	)
	private void optic$vanillaHotbarItem(InGameHud self, int slot, int x, int y, float tickDelta) {
		if (OpticClient.get().config().hotbar) return;
		this.renderActionBar(slot, x, y, tickDelta);
	}

	@Unique private boolean optic$statusMoved;

	/**
	 * Hotbar module: hearts, armor, food, air and mount health (method_5587), plus the jump bar,
	 * XP bar and level, are drawn relative to where vanilla puts the hotbar. Draw them with the
	 * same move / scale / rotation as Optic's hotbar so they stay attached to it.
	 */
	@Inject(method = "render(FZII)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;method_5587(II)V"), require = 0)
	private void optic$statusBarsBegin(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
		optic$beginHotbarTransform();
	}

	@Inject(method = "render(FZII)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;method_5587(II)V", shift = At.Shift.AFTER), require = 0)
	private void optic$statusBarsEnd(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
		optic$endHotbarTransform();
	}

	@Inject(method = "render(FZII)V", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V", args = "ldc=jumpBar", shift = At.Shift.AFTER), require = 0)
	private void optic$jumpBarBegin(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
		optic$beginHotbarTransform();
	}

	@Inject(method = "render(FZII)V", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V", args = "ldc=expBar", shift = At.Shift.AFTER), require = 0)
	private void optic$expBarBegin(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
		optic$beginHotbarTransform();
	}

	@Inject(method = "render(FZII)V", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V", args = "ldc=expLevel", shift = At.Shift.AFTER), require = 0)
	private void optic$expLevelBegin(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
		optic$beginHotbarTransform();
	}

	/**
	 * Item Info (the held item's name over the hotbar) moves, scales and rotates with the
	 * hotbar too. When the status icons flip under the bar, it goes under them.
	 */
	@Inject(method = "render(FZII)V", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V", args = "ldc=toolHighlight", shift = At.Shift.AFTER), require = 0)
	private void optic$itemInfoBegin(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
		optic$beginHotbarTransform(ITEM_INFO_FLIP);
	}

	/** Each of those sections ends with a profiler pop; close the transform there. */
	@Inject(method = "render(FZII)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V"), require = 0)
	private void optic$sectionEnd(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
		optic$endHotbarTransform();
	}

	@Unique
	private void optic$beginHotbarTransform() {
		optic$beginHotbarTransform(STATUS_FLIP);
	}

	@Unique
	private void optic$beginHotbarTransform(int flipShift) {
		if (optic$statusMoved) return;
		OpticConfig c = OpticClient.get().config();
		if (!c.hotbar) return;
		Window window = new Window(client, client.width, client.height);
		float scale = Math.max(0.5F, Math.min(3.0F, c.hotbarScale));
		int rot = OpticConfig.normalizeHotbarRotation(c.hotbarRotation);
		int baseX = OpticHudLayout.hotbarX(c, window);
		int baseY = OpticHudLayout.hotbarY(c, window);
		GlStateManager.pushMatrix();
		GlStateManager.translate(baseX, baseY, 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		optic$applyHotbarRotation(rot);
		// Hearts/food/XP normally sit above the bar. If that side would run off screen (bar moved
		// to the top, or rotated against an edge), put them on the other side of the bar.
		if (optic$overflow(STATUS_ABOVE_TOP, baseX, baseY, scale, rot, window)
			> optic$overflow(STATUS_ABOVE_TOP + STATUS_FLIP, baseX, baseY, scale, rot, window)) {
			GlStateManager.translate(0.0F, flipShift, 0.0F);
		}
		// Vanilla's hotbar origin becomes Optic's hotbar origin.
		GlStateManager.translate(-(window.getWidth() / 2 - 91), -(window.getHeight() - 22), 0.0F);
		optic$statusMoved = true;
	}

	/** Status icons span hotbar-local y -27..-1 (two heart rows to the XP bar). */
	@Unique private static final int STATUS_ABOVE_TOP = -27;
	@Unique private static final int STATUS_H = 27;
	/** Moves that block to just under the bar: -27 + 40 = 13, below the 22px bar. */
	@Unique private static final int STATUS_FLIP = 40;
	/** Item name sits at local y -37; under the flipped status block (13..40) it goes to 42. */
	@Unique private static final int ITEM_INFO_FLIP = 79;

	/** How far (in screen pixels) the status block starting at local y would stick out of the screen. */
	@Unique
	private static float optic$overflow(int localTop, int baseX, int baseY, float scale, int rot, Window window) {
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		float[][] corners = {
			{0, localTop}, {OpticHudLayout.HOTBAR_W, localTop},
			{0, localTop + STATUS_H}, {OpticHudLayout.HOTBAR_W, localTop + STATUS_H}
		};
		for (float[] p : corners) {
			float[] box = OpticHudLayout.hotbarLocalToBox(p[0], p[1], rot);
			float sx = baseX + box[0] * scale;
			float sy = baseY + box[1] * scale;
			minX = Math.min(minX, sx);
			maxX = Math.max(maxX, sx);
			minY = Math.min(minY, sy);
			maxY = Math.max(maxY, sy);
		}
		float out = 0.0F;
		out += Math.max(0.0F, -minX) + Math.max(0.0F, maxX - window.getWidth());
		out += Math.max(0.0F, -minY) + Math.max(0.0F, maxY - window.getHeight());
		return out;
	}

	/** Hotbar module part toggles for hearts + armor (+ mount) and hunger + air. */
	@Redirect(
		method = "method_5587(II)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V"),
		require = 0
	)
	private void optic$statusIcon(InGameHud self, int x, int y, int u, int v, int width, int height) {
		OpticConfig c = OpticClient.get().config();
		if (c.hotbar) {
			boolean hunger = v == 27 || v == 18; // food, air
			boolean hearts = v == 0 || v == 45 || v == 9; // hearts (hardcore), armor, mount
			if (hunger && !c.hotbarShowHunger) return;
			if (hearts && !c.hotbarShowHearts) return;
		}
		self.drawTexture(x, y, u, v, width, height);
	}

	@Unique private boolean optic$hideLevel;

	/** XP level number: hidden with the XP toggle. Only active inside the expLevel section. */
	@Redirect(
		method = "render(FZII)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;III)I"),
		require = 0
	)
	private int optic$levelText(TextRenderer renderer, String text, int x, int y, int color) {
		if (optic$hideLevel) return 0;
		return renderer.draw(text, x, y, color);
	}

	@Inject(method = "render(FZII)V", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V", args = "ldc=expLevel"), require = 0)
	private void optic$levelBegin(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		optic$hideLevel = c.hotbar && !c.hotbarShowXp;
	}

	@Unique
	private void optic$endHotbarTransform() {
		optic$hideLevel = false;
		if (!optic$statusMoved) return;
		GlStateManager.popMatrix();
		optic$statusMoved = false;
	}

	/** Hotbar module: moved, scaled and rotated bar with upright items, as on 1.8.9. */
	@Unique
	private void optic$drawHotbar(OpticConfig c, Window window, float tickDelta) {
		if (!(client.field_6279 instanceof PlayerEntity)) return;
		if (!c.hotbarShowSlots) return;
		PlayerEntity player = (PlayerEntity) client.field_6279;
		float scale = Math.max(0.5F, Math.min(3.0F, c.hotbarScale));
		int rot = OpticConfig.normalizeHotbarRotation(c.hotbarRotation);
		int baseX = OpticHudLayout.hotbarX(c, window);
		int baseY = OpticHudLayout.hotbarY(c, window);

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		client.getTextureManager().bindTexture(OPTIC_WIDGETS);
		GlStateManager.pushMatrix();
		GlStateManager.translate(baseX, baseY, 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		optic$applyHotbarRotation(rot);
		float z = this.zOffset;
		this.zOffset = -90.0F;
		this.drawTexture(0, 0, 0, 0, 182, 22);
		this.drawTexture(-1 + player.inventory.selectedSlot * 20, -1, 0, 22, 24, 22);
		this.zOffset = z;
		GlStateManager.popMatrix();

		GlStateManager.enableRescaleNormal();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		DiffuseLighting.enable();
		for (int slot = 0; slot < 9; slot++) {
			float[] p = OpticHudLayout.hotbarLocalToBox(3 + slot * 20 + 8, 3 + 8, rot);
			int ix = Math.round(baseX + (p[0] - 8.0F) * scale);
			int iy = Math.round(baseY + (p[1] - 8.0F) * scale);
			GlStateManager.pushMatrix();
			GlStateManager.translate(ix, iy, 0.0F);
			GlStateManager.scale(scale, scale, 1.0F);
			this.renderActionBar(slot, 0, 0, tickDelta);
			GlStateManager.popMatrix();
		}
		DiffuseLighting.disable();
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	@Unique
	private static void optic$applyHotbarRotation(int rot) {
		switch (rot) {
			case 90:
				GlStateManager.translate(OpticHudLayout.HOTBAR_H, 0.0F, 0.0F);
				GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
				break;
			case 180:
				GlStateManager.translate(OpticHudLayout.HOTBAR_W, OpticHudLayout.HOTBAR_H, 0.0F);
				GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
				break;
			case 270:
				GlStateManager.translate(0.0F, OpticHudLayout.HOTBAR_W, 0.0F);
				GlStateManager.rotate(270.0F, 0.0F, 0.0F, 1.0F);
				break;
			default:
				break;
		}
	}

	/** Scoreboard module. method_4943 is 1.7's sidebar renderer (objective, height, width, font). */
	@Inject(method = "method_4943", at = @At("HEAD"), cancellable = true)
	private void optic$scoreboard(ScoreboardObjective objective, int screenHeight, int screenWidth, TextRenderer fr, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.scoreboard) return;
		ci.cancel();

		Scoreboard scoreboard = objective.getScoreboard();
		@SuppressWarnings("unchecked")
		Collection<ScoreboardPlayerScore> all = (Collection<ScoreboardPlayerScore>) scoreboard.getAllPlayerScores(objective);
		List<ScoreboardPlayerScore> list = Lists.newArrayList(Iterables.filter(all, new Predicate<ScoreboardPlayerScore>() {
			@Override
			public boolean apply(ScoreboardPlayerScore score) {
				return score.getPlayerName() != null && !score.getPlayerName().startsWith("#");
			}
		}));
		Collection<ScoreboardPlayerScore> collection = list.size() > 15 ? Lists.newArrayList(Iterables.skip(list, list.size() - 15)) : list;

		boolean showNumbers = c.scoreboardShowNumbers;
		boolean shadow = c.scoreboardTextShadow;
		float opacity = Math.max(0.0F, Math.min(100.0F, c.scoreboardBackgroundOpacity)) / 100.0F;

		int width = fr.getStringWidth(objective.getDisplayName());
		for (ScoreboardPlayerScore score : collection) {
			String name = Team.decorateName(scoreboard.getPlayerTeam(score.getPlayerName()), score.getPlayerName());
			int rowW = fr.getStringWidth(name);
			if (showNumbers && !c.scoreboardRemoveNumberWidth) rowW = fr.getStringWidth(name + ": " + Formatting.RED + score.getScore());
			width = Math.max(width, rowW);
		}

		int listH = collection.size() * fr.fontHeight;
		int left;
		int right;
		int baseY;
		if (c.scoreboardX >= 0 && c.scoreboardY >= 0) {
			left = c.scoreboardX + 2;
			baseY = c.scoreboardY + (collection.size() + 1) * fr.fontHeight + 1;
			right = left + width + 2;
		} else {
			baseY = screenHeight / 2 + listH / 3;
			left = screenWidth - width - 3;
			right = screenWidth - 3 + 2;
		}
		int index = 0;
		for (ScoreboardPlayerScore score : collection) {
			index++;
			String name = Team.decorateName(scoreboard.getPlayerTeam(score.getPlayerName()), score.getPlayerName());
			String number = Formatting.RED + "" + score.getScore();
			int y = baseY - index * fr.fontHeight;
			optic$fillScoreboardBg(left - 2, y, right, y + fr.fontHeight, 0x50000000, opacity);
			fr.draw(name, left, y, 0xFFFFFFFF, shadow);
			if (showNumbers) fr.draw(number, right - fr.getStringWidth(number), y, 0xFFFFFFFF, shadow);
			if (index == collection.size()) {
				String title = objective.getDisplayName();
				optic$fillScoreboardBg(left - 2, y - fr.fontHeight - 1, right, y - 1, 0x60000000, opacity);
				optic$fillScoreboardBg(left - 2, y - 1, right, y, 0x50000000, opacity);
				fr.draw(title, left + width / 2 - fr.getStringWidth(title) / 2, y - fr.fontHeight, 0xFFFFFFFF, shadow);
			}
		}
	}

	@Unique
	private static void optic$fillScoreboardBg(int x1, int y1, int x2, int y2, int argb, float opacity) {
		if (opacity <= 0.0F) return;
		int a = Math.min(255, Math.round(((argb >>> 24) & 0xFF) * opacity));
		fill(x1, y1, x2, y2, (a << 24) | (argb & 0xFFFFFF));
	}

	@Unique
	private void optic$text(String text, int x, int y, int textRgb, boolean opticFont, boolean shadow, float scale) {
		int color = textRgb & 0xFFFFFF;
		int argb = 0xFF000000 | color;
		if (opticFont) {
			float size = OpticHudLayout.fontSize(scale);
			if (shadow) OpticUiFont.drawSized(client, text, x + 1, y + 1, 0xAA000000, size);
			OpticUiFont.drawSized(client, text, x, y, argb, size);
			return;
		}
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		client.textRenderer.draw(text, 0, 0, color, shadow);
		GlStateManager.popMatrix();
	}
}
