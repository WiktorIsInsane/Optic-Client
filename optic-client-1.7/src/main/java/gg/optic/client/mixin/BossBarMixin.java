package gg.optic.client.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.hud.OpticBossBar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.Window;
import net.minecraft.entity.boss.BossBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Boss Bar mod: hide it, move it, scale it, or hide just the name or the health bar.
 * Vanilla's own drawing (and its lifetime countdown) still runs, just moved and scaled.
 */
@Mixin(InGameHud.class)
public abstract class BossBarMixin {
	@Shadow @Final private MinecraftClient client;

	@Unique private boolean optic$moved;

	@Inject(method = "renderBossBar()V", at = @At("HEAD"), cancellable = true, require = 0)
	private void optic$begin(CallbackInfo ci) {
		optic$moved = false;
		OpticConfig c = OpticClient.get().config();
		if (!c.bossBar) return;
		if (!c.bossBarShow) {
			// Hidden: still count the bar down so it doesn't linger when shown again.
			if (BossBar.name != null && BossBar.framesToLive > 0) BossBar.framesToLive--;
			ci.cancel();
			return;
		}
		Window window = new Window(client, client.width, client.height);
		float scale = OpticBossBar.scale(c);
		GlStateManager.pushMatrix();
		GlStateManager.translate(OpticBossBar.x(c, window), OpticBossBar.y(c), 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		// Vanilla's bar starts at (width / 2 - 91, 2): make that our origin.
		GlStateManager.translate(-(window.getWidth() / 2 - 91), -2.0F, 0.0F);
		optic$moved = true;
	}

	@Inject(method = "renderBossBar()V", at = @At("RETURN"), require = 0)
	private void optic$end(CallbackInfo ci) {
		if (!optic$moved) return;
		optic$moved = false;
		GlStateManager.popMatrix();
	}

	@Redirect(
		method = "renderBossBar()V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V"),
		require = 0
	)
	private void optic$bar(InGameHud self, int x, int y, int u, int v, int width, int height) {
		OpticConfig c = OpticClient.get().config();
		if (c.bossBar && !c.bossBarShowBar) return;
		self.drawTexture(x, y, u, v, width, height);
	}

	@Redirect(
		method = "renderBossBar()V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;method_956(Ljava/lang/String;III)I"),
		require = 0
	)
	private int optic$name(TextRenderer renderer, String text, int x, int y, int color) {
		OpticConfig c = OpticClient.get().config();
		if (c.bossBar && !c.bossBarShowName) return 0;
		return renderer.method_956(text, x, y, color);
	}
}
