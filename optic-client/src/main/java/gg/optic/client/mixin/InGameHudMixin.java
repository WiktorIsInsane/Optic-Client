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
import gg.optic.client.render.OpticHitPredict;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin extends DrawableHelper {
	@Unique private static final Identifier OPTIC_WIDGETS = new Identifier("textures/gui/widgets.png");

	@Shadow @Final private MinecraftClient client;

	@Shadow
	public abstract TextRenderer getFontRenderer();

	@Shadow
	protected abstract void renderHotbarItem(int slot, int x, int y, float tickDelta, PlayerEntity playerEntity);

	@Shadow private int heldItemTooltipFade;
	@Shadow private ItemStack heldItem;

	@Unique private final float[] optic$keyPress = new float[6];
	@Unique private long optic$lastKeyFrame = 0L;

	/** Vanilla gap between hotbar top (height-22) and item-name Y (height-59). */
	@Unique private static final int OPTIC_ITEM_INFO_GAP = 37;

	@Inject(method = "renderHeldItemName", at = @At("HEAD"), cancellable = true)
	private void optic$itemInfo(Window window, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.itemInfo) {
			ci.cancel();
			return;
		}
		// When the hotbar mod is on, keep the name at the vanilla gap above the (possibly moved) bar.
		if (!c.hotbar) return;
		ci.cancel();

		if (this.heldItemTooltipFade <= 0 || this.heldItem == null) return;

		String name = this.heldItem.getCustomName();
		if (this.heldItem.hasCustomName()) {
			name = Formatting.ITALIC + name;
		}

		int barX = OpticHudLayout.hotbarX(c, window);
		int barY = OpticHudLayout.hotbarY(c, window);
		int barW = OpticHudLayout.hotbarDrawW(c);
		int x = barX + barW / 2 - this.getFontRenderer().getStringWidth(name) / 2;
		int y = barY - OPTIC_ITEM_INFO_GAP;
		if (!this.client.interactionManager.hasStatusBars()) {
			y += 14;
		}

		int alpha = (int) (this.heldItemTooltipFade * 256.0F / 10.0F);
		if (alpha > 255) alpha = 255;
		if (alpha <= 0) return;

		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		this.getFontRenderer().drawWithShadow(name, (float) x, (float) y, 16777215 + (alpha << 24));
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	@Inject(method = "render(F)V", at = @At("TAIL"))
	private void optic$hitPredictLabel(float tickDelta, CallbackInfo ci) {
		if (client.options.hudHidden || client.player == null || OpticGui.isModMenuOpen()) return;
		String label = OpticHitPredict.hudLabel();
		if (label == null) return;
		Window window = new Window(client);
		int x = window.getWidth() / 2;
		int y = window.getHeight() / 2 + 12;
		int width = this.getFontRenderer().getStringWidth(label);
		this.getFontRenderer().drawWithShadow(label, x - width / 2.0F, y, OpticHitPredict.hudColor());
	}

	@Inject(method = "render(F)V", at = @At("TAIL"))
	private void optic$render(float tickDelta, CallbackInfo ci) {
		if (client.options.debugEnabled || client.options.hudHidden) return;
		// Hide live HUD while the mod click-GUI is open; edit GUI still needs it for layout.
		if (OpticGui.isModMenuOpen()) return;
		OpticConfig config = OpticClient.get().config();
		boolean freezeInput = OpticGui.isOpticScreenOpen();
		if (config.fps) {
			String text = config.fpsShowLabel
				? "FPS  " + MinecraftClient.getCurrentFps()
				: Integer.toString(MinecraftClient.getCurrentFps());
			drawHudText(
				OpticConfig.withBrackets(text, config.fpsBrackets),
				config.fpsX,
				config.fpsY,
				config.fpsTextColor,
				config.fpsOpticFont,
				config.fpsTextShadow,
				config.fpsScale
			);
		}
		if (config.cps) {
			String cpsValue = OpticClient.get().clicks().leftCps() + " | " + OpticClient.get().clicks().rightCps();
			if (!config.cpsSeparateButtons) {
				cpsValue = Integer.toString(
					OpticClient.get().clicks().leftCps() + OpticClient.get().clicks().rightCps()
				);
			}
			String text = config.cpsShowLabel ? "CPS  " + cpsValue : cpsValue;
			drawHudText(
				OpticConfig.withBrackets(text, config.cpsBrackets),
				config.cpsX,
				config.cpsY,
				config.cpsTextColor,
				config.cpsOpticFont,
				config.cpsTextShadow,
				config.cpsScale
			);
		}
		if (config.ping && client.player != null && client.getNetworkHandler() != null) {
			PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
			if (entry != null) {
				String value = Integer.toString(entry.getLatency());
				if (config.pingShowMs) value = value + "ms";
				String text = config.pingShowLabel ? "PING  " + value : value;
				drawHudText(
					OpticConfig.withBrackets(text, config.pingBrackets),
					config.pingX,
					config.pingY,
					config.pingTextColor,
					config.pingOpticFont,
					config.pingTextShadow,
					config.pingScale
				);
			}
		}
		if (config.coordinates && client.player != null) {
			String format = "%." + config.coordinatesDecimals + "f";
			String value;
			if (config.coordinatesShowY) {
				value = String.format(
					format + " / " + format + " / " + format,
					client.player.x,
					client.player.y,
					client.player.z
				);
			} else {
				value = String.format(format + " / " + format, client.player.x, client.player.z);
			}
			String text = config.coordinatesShowLabel ? "XYZ  " + value : value;
			drawHudText(
				OpticConfig.withBrackets(text, config.coordinatesBrackets),
				config.coordinatesX,
				config.coordinatesY,
				config.coordinatesTextColor,
				config.coordinatesOpticFont,
				config.coordinatesTextShadow,
				config.coordinatesScale
			);
		}
		if (config.toggleSprint && config.toggleSprintShowHud) {
			drawHudText(
				OpticConfig.withBrackets("SPRINT", config.toggleSprintBrackets),
				config.toggleSprintX,
				config.toggleSprintY,
				config.toggleSprintTextColor,
				config.toggleSprintOpticFont,
				config.toggleSprintTextShadow,
				config.toggleSprintScale
			);
		}
		if (config.keystrokes) renderKeystrokes(config, freezeInput);
	}

	private void drawHudText(
		String text,
		int x,
		int y,
		int textRgb,
		boolean opticFont,
		boolean shadow,
		float scale
	) {
		int color = textRgb & 0xFFFFFF;
		int argb = 0xFF000000 | color;
		float size = OpticHudLayout.fontSize(scale);
		if (opticFont) {
			if (shadow) {
				OpticUiFont.drawSized(client, text, x + 1, y + 1, 0xAA000000, size);
			}
			OpticUiFont.drawSized(client, text, x, y, argb, size);
		} else if (Math.abs(scale - 1.0F) > 0.01F) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(x, y, 0.0F);
			GlStateManager.scale(scale, scale, 1.0F);
			if (shadow) {
				drawWithShadow(client.textRenderer, text, 0, 0, color);
			} else {
				client.textRenderer.draw(text, 0, 0, color);
			}
			GlStateManager.popMatrix();
		} else if (shadow) {
			drawWithShadow(client.textRenderer, text, x, y, color);
		} else {
			client.textRenderer.draw(text, x, y, color);
		}
	}

	private void renderKeystrokes(OpticConfig config, boolean freezeInput) {
		long now = System.currentTimeMillis();
		float dt = optic$lastKeyFrame == 0L ? 0.016F : Math.min(0.05F, (now - optic$lastKeyFrame) / 1000.0F);
		optic$lastKeyFrame = now;

		key(0, "W", client.options.forwardKey, config, dt, false, 0, freezeInput);
		key(1, "A", client.options.leftKey, config, dt, false, 0, freezeInput);
		key(2, "S", client.options.backKey, config, dt, false, 0, freezeInput);
		key(3, "D", client.options.rightKey, config, dt, false, 0, freezeInput);
		if (config.keystrokesShowMouse) {
			key(4, "LMB", client.options.attackKey, config, dt, true, OpticClient.get().clicks().leftCps(), freezeInput);
			key(5, "RMB", client.options.useKey, config, dt, true, OpticClient.get().clicks().rightCps(), freezeInput);
		}
	}

	private void key(
		int index,
		String name,
		KeyBinding binding,
		OpticConfig config,
		float dt,
		boolean mouse,
		int cps,
		boolean freezeInput
	) {
		int x = OpticHudLayout.keyX(config, client, index);
		int y = OpticHudLayout.keyY(config, client, index);
		int width = OpticHudLayout.keyW(config, index);
		int height = OpticHudLayout.keyH(config, index);

		boolean down = !freezeInput && (binding.getCode() < 0
			? org.lwjgl.input.Mouse.isButtonDown(binding.getCode() + 100)
			: Keyboard.isKeyDown(binding.getCode()));
		if (freezeInput) {
			optic$keyPress[index] = 0.0F;
		}

		float fadeSec = Math.max(0.05F, config.keystrokesFadeMs / 1000.0F);
		float amount = optic$keyPress[index];
		if (down) {
			if (config.keystrokesInstant || !config.keystrokesFade) {
				amount = 1.0F;
			} else {
				amount = Math.min(1.0F, amount + dt / Math.max(0.05F, fadeSec * 0.35F));
			}
		} else if (config.keystrokesFade) {
			amount = Math.max(0.0F, amount - dt / fadeSec);
		} else {
			amount = 0.0F;
		}
		optic$keyPress[index] = amount;

		if (config.keystrokesBackground) {
			int idle = config.keystrokesModernStyle ? 0xAA101210 : 0x88000000;
			int pressed = config.keystrokesModernStyle
				? OpticConfig.withAlpha(config.keystrokesColor, 0xDD)
				: 0xAAFFFFFF;
			int bg = blendArgb(idle, pressed, amount);
			fill(x, y, x + width, y + height, bg);
		} else if (amount > 0.02F) {
			int pressed = OpticConfig.withAlpha(config.keystrokesColor, (int) (0xDD * amount));
			fill(x, y, x + width, y + height, pressed);
		}

		String line1 = "";
		String line2 = null;
		boolean stacked = false;
		if (mouse) {
			int mode = config.keystrokesCpsMode;
			boolean clicking = down || amount > 0.15F;
			if (mode == 1) {
				line1 = Integer.toString(cps);
			} else if (mode == 2) {
				line1 = config.keystrokesShowLabels ? name : "";
				line2 = Integer.toString(cps);
				stacked = true;
			} else if (mode == 3) {
				line1 = clicking ? Integer.toString(cps) : (config.keystrokesShowLabels ? name : "");
			} else {
				line1 = config.keystrokesShowLabels ? name : "";
			}
		} else {
			line1 = config.keystrokesShowLabels ? name : "";
		}

		if (line1.isEmpty() && (line2 == null || line2.isEmpty())) return;

		int color = config.keystrokesTextColor & 0xFFFFFF;
		int argb = 0xFF000000 | color;
		float font = stacked
			? OpticHudLayout.mouseStackedFont(config.keystrokesScale)
			: OpticHudLayout.fontSize(config.keystrokesScale);
		if (stacked) {
			int gap = Math.max(1, Math.round(1.0F * config.keystrokesScale));
			int lineH = Math.max(6, Math.round(font + 1.0F));
			int total = lineH * 2 + gap;
			int top = y + Math.max(1, (height - total) / 2);
			drawKeyLine(line1, x, top, width, config, color, argb, font);
			drawKeyLine(line2, x, top + lineH + gap, width, config, color, argb, font);
		} else {
			int textH = Math.max(8, Math.round(font));
			drawKeyLine(line1, x, y + (height - textH) / 2, width, config, color, argb, font);
		}
	}

	private void drawKeyLine(
		String text,
		int x,
		int y,
		int width,
		OpticConfig config,
		int color,
		int argb,
		float font
	) {
		if (text == null || text.isEmpty()) return;
		if (config.keystrokesOpticFont) {
			int textW = OpticUiFont.widthSized(client, text, argb, font);
			int drawX = x + (width - textW) / 2;
			if (config.keystrokesTextShadow) {
				OpticUiFont.drawSized(client, text, drawX + 1, y + 1, 0xAA000000, font);
			}
			OpticUiFont.drawSized(client, text, drawX, y, argb, font);
		} else {
			float scale = font / OpticHudLayout.BASE_FONT;
			int textW = client.textRenderer.getStringWidth(text);
			int drawX = x + (width - Math.round(textW * scale)) / 2;
			if (Math.abs(scale - 1.0F) > 0.01F) {
				GlStateManager.pushMatrix();
				GlStateManager.translate(drawX, y, 0.0F);
				GlStateManager.scale(scale, scale, 1.0F);
				if (config.keystrokesTextShadow) {
					drawWithShadow(client.textRenderer, text, 0, 0, color);
				} else {
					client.textRenderer.draw(text, 0, 0, color);
				}
				GlStateManager.popMatrix();
			} else if (config.keystrokesTextShadow) {
				drawWithShadow(client.textRenderer, text, drawX, y, color);
			} else {
				client.textRenderer.draw(text, drawX, y, color);
			}
		}
	}

	@Unique
	private static int blendArgb(int from, int to, float t) {
		t = Math.max(0.0F, Math.min(1.0F, t));
		int af = (from >> 24) & 0xFF;
		int rf = (from >> 16) & 0xFF;
		int gf = (from >> 8) & 0xFF;
		int bf = from & 0xFF;
		int at = (to >> 24) & 0xFF;
		int rt = (to >> 16) & 0xFF;
		int gt = (to >> 8) & 0xFF;
		int bt = to & 0xFF;
		int a = (int) (af + (at - af) * t);
		int r = (int) (rf + (rt - rf) * t);
		int g = (int) (gf + (gt - gf) * t);
		int b = (int) (bf + (bt - bf) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	@Inject(method = "renderScoreboardObjective", at = @At("HEAD"), cancellable = true)
	private void optic$scoreboard(ScoreboardObjective objective, Window window, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.scoreboard) return;
		ci.cancel();

		Scoreboard scoreboard = objective.getScoreboard();
		Collection<ScoreboardPlayerScore> collection = scoreboard.getAllPlayerScores(objective);
		List<ScoreboardPlayerScore> list = Lists.newArrayList(Iterables.filter(collection, new Predicate<ScoreboardPlayerScore>() {
			@Override
			public boolean apply(ScoreboardPlayerScore score) {
				return score.getPlayerName() != null && !score.getPlayerName().startsWith("#");
			}
		}));
		if (list.size() > 15) {
			collection = Lists.newArrayList(Iterables.skip(list, collection.size() - 15));
		} else {
			collection = list;
		}

		TextRenderer fr = this.getFontRenderer();
		boolean showNumbers = c.scoreboardShowNumbers;
		boolean removeNumberWidth = c.scoreboardRemoveNumberWidth;
		boolean shadow = c.scoreboardTextShadow;
		float opacity = Math.max(0.0F, Math.min(100.0F, c.scoreboardBackgroundOpacity)) / 100.0F;

		int width = fr.getStringWidth(objective.getDisplayName());
		for (ScoreboardPlayerScore score : collection) {
			Team team = scoreboard.getPlayerTeam(score.getPlayerName());
			String name = Team.decorateName(team, score.getPlayerName());
			int rowW = fr.getStringWidth(name);
			if (showNumbers && !removeNumberWidth) {
				rowW = fr.getStringWidth(name + ": " + Formatting.RED + score.getScore());
			}
			width = Math.max(width, rowW);
		}

		int listH = collection.size() * fr.fontHeight;
		int rightPad = 3;
		int left;
		int right;
		int baseY;
		if (c.scoreboardX >= 0 && c.scoreboardY >= 0) {
			// scoreboardX/Y = top-left of the full box (title + rows)
			left = c.scoreboardX + 2;
			baseY = c.scoreboardY + (collection.size() + 1) * fr.fontHeight + 1;
			right = left + width + 2;
		} else {
			baseY = window.getHeight() / 2 + listH / 3;
			left = window.getWidth() - width - rightPad;
			right = window.getWidth() - rightPad + 2;
		}
		int index = 0;

		for (ScoreboardPlayerScore score : collection) {
			index++;
			Team team = scoreboard.getPlayerTeam(score.getPlayerName());
			String name = Team.decorateName(team, score.getPlayerName());
			String number = Formatting.RED + "" + score.getScore();
			int y = baseY - index * fr.fontHeight;

			optic$fillScoreboardBg(left - 2, y, right, y + fr.fontHeight, 0x50000000, opacity);
			optic$drawScoreboardText(fr, name, left, y, 0xFFFFFFFF, shadow);
			if (showNumbers) {
				optic$drawScoreboardText(fr, number, right - fr.getStringWidth(number), y, 0xFFFFFFFF, shadow);
			}

			if (index == collection.size()) {
				String title = objective.getDisplayName();
				optic$fillScoreboardBg(left - 2, y - fr.fontHeight - 1, right, y - 1, 0x60000000, opacity);
				optic$fillScoreboardBg(left - 2, y - 1, right, y, 0x50000000, opacity);
				optic$drawScoreboardText(
					fr,
					title,
					left + width / 2 - fr.getStringWidth(title) / 2,
					y - fr.fontHeight,
					0xFFFFFFFF,
					shadow
				);
			}
		}
	}

	@Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
	private void optic$hotbar(Window window, float tickDelta, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.hotbar) return;
		if (!(this.client.getCameraEntity() instanceof PlayerEntity)) return;
		ci.cancel();

		PlayerEntity player = (PlayerEntity) this.client.getCameraEntity();
		float scale = Math.max(0.5F, Math.min(3.0F, c.hotbarScale));
		int rot = OpticConfig.normalizeHotbarRotation(c.hotbarRotation);
		int baseX = OpticHudLayout.hotbarX(c, window);
		int baseY = OpticHudLayout.hotbarY(c, window);

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.client.getTextureManager().bindTexture(OPTIC_WIDGETS);

		// Rotated bar + selection (items rendered upright separately).
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

		// Place upright 16x16 icons on the transformed slot centers (not rotated corners).
		for (int slot = 0; slot < 9; slot++) {
			float cx = 3 + slot * 20 + 8;
			float cy = 3 + 8;
			float[] p = OpticHudLayout.hotbarLocalToBox(cx, cy, rot);
			int ix = Math.round(baseX + (p[0] - 8.0F) * scale);
			int iy = Math.round(baseY + (p[1] - 8.0F) * scale);
			GlStateManager.pushMatrix();
			GlStateManager.translate(ix, iy, 0.0F);
			GlStateManager.scale(scale, scale, 1.0F);
			this.renderHotbarItem(slot, 0, 0, tickDelta, player);
			GlStateManager.popMatrix();
		}

		DiffuseLighting.disable();
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableBlend();
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

	@Unique
	private static void optic$fillScoreboardBg(int x1, int y1, int x2, int y2, int argb, float opacity) {
		if (opacity <= 0.0F) return;
		int a = Math.min(255, Math.round(((argb >>> 24) & 0xFF) * opacity));
		fill(x1, y1, x2, y2, (a << 24) | (argb & 0xFFFFFF));
	}

	@Unique
	private static void optic$drawScoreboardText(TextRenderer fr, String text, int x, int y, int color, boolean shadow) {
		if (shadow) {
			fr.drawWithShadow(text, (float) x, (float) y, color);
		} else {
			fr.draw(text, x, y, color);
		}
	}
}
