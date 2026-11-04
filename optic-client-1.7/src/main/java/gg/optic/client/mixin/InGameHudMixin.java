package gg.optic.client.mixin;

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
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws Optic's HUD modules on top of the 1.7.10 in-game HUD. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Shadow @Final private MinecraftClient client;

	@Inject(method = "render(FZII)V", at = @At("TAIL"))
	private void optic$render(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
		if (client.options.debugEnabled || client.options.hudHidden) return;
		// Hide the live HUD while the mod menu is open; the HUD editor still needs it.
		if (OpticGui.isModMenuOpen()) return;
		OpticConfig config = OpticClient.get().config();
		boolean freezeInput = OpticGui.isOpticScreenOpen();
		// 1.7 leaves its last HUD draw's state behind (no GlStateManager to reset it).
		GlStateManager.enableTexture();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GlStateManager.enableAlphaTest();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		if (config.fps) OpticHud.drawFps(client, config);
		if (config.cps) OpticHud.drawCps(client, config);
		if (config.ping) OpticHud.drawPing(client, config);
		if (config.coordinates) OpticHud.drawCoordinates(client, config);
		OpticPotions.render(client);
		Window window = new Window(client, client.width, client.height);
		OpticCrosshair.render(client, window.getWidth() / 2, window.getHeight() / 2);
		if (config.toggleSprint && config.toggleSprintShowHud) {
			optic$text(
				OpticConfig.withBrackets("SPRINT", config.toggleSprintBrackets),
				config.toggleSprintX,
				config.toggleSprintY,
				config.toggleSprintTextColor,
				config.toggleSprintOpticFont,
				config.toggleSprintTextShadow,
				config.toggleSprintScale
			);
		}
		if (config.keystrokes) OpticKeystrokes.render(client, config, freezeInput);
		if (OpticTab.visible(client) && client.world != null) {
			OpticTab.render(client, window.getWidth(), client.world.getScoreboard(), client.world.getScoreboard().getObjectiveForSlot(0));
		}
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/** 1.7 draws the tab list inline in render; hide it while Optic's Tab module draws its own. */
	@Redirect(
		method = "render(FZII)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z")
	)
	private boolean optic$vanillaTab(KeyBinding binding) {
		if (OpticClient.get().config().tab) return false;
		return binding.isPressed();
	}

	/** The vanilla crosshair is the 16x16 icon at u=0, v=0; skip it when Optic draws its own. */
	@Redirect(
		method = "render(FZII)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V")
	)
	private void optic$vanillaCrosshair(InGameHud self, int x, int y, int u, int v, int width, int height) {
		boolean crosshair = u == 0 && v == 0 && width == 16 && height == 16;
		if (crosshair && OpticCrosshair.hidesVanilla(OpticClient.get().config())) return;
		self.drawTexture(x, y, u, v, width, height);
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
