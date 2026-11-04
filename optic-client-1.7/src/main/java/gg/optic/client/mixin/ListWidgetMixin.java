package gg.optic.client.mixin;

import gg.optic.client.OpticSmooth;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ListWidget;
import net.minecraft.client.render.Tessellator;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the world visible through the list and clips rows so they stay inside it. */
@Mixin(ListWidget.class)
public abstract class ListWidgetMixin {
	@Shadow
	@Final
	protected MinecraftClient client;

	@Shadow
	protected int xStart;

	@Shadow
	protected int xEnd;

	@Shadow
	protected int yStart;

	@Shadow
	protected int yEnd;

	private boolean optic$clipped;

	@Inject(method = "renderList(IIII)V", at = @At("HEAD"))
	private void optic$clipOn(int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
		optic$clipped = false;
		if (!optic$browser()) return;
		int sf = OpticSmooth.scale(client);
		int sx = xStart * sf;
		int sy = client.height - yEnd * sf;
		int sw = Math.max(0, (xEnd - xStart) * sf);
		int sh = Math.max(0, (yEnd - yStart) * sf);
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(sx, sy, sw, sh);
		optic$clipped = true;
	}

	@Inject(method = "renderList(IIII)V", at = @At("RETURN"))
	private void optic$clipOff(int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
		if (!optic$clipped) return;
		optic$clipped = false;
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
	}

	/** The list body is one dirt quad. Drop it so the world behind the screen stays visible. */
	@Redirect(
		method = "render(IIF)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/Tessellator;end()I",
			ordinal = 0
		)
	)
	private int optic$skipDirt(Tessellator tessellator) {
		if (optic$browser()) {
			// 1.7 cannot discard a started quad; finish it with colour writes off.
			GL11.glColorMask(false, false, false, false);
			int drawn = tessellator.end();
			GL11.glColorMask(true, true, true, true);
			return drawn;
		}
		return tessellator.end();
	}

	@Inject(method = "renderHoleBackground(IIII)V", at = @At("HEAD"), cancellable = true)
	private void optic$clearBars(int y1, int y2, int startAlpha, int endAlpha, CallbackInfo ci) {
		if (optic$browser()) ci.cancel();
	}

	private boolean optic$browser() {
		return client != null && (client.currentScreen instanceof MultiplayerScreen || client.currentScreen instanceof SelectWorldScreen);
	}
}
