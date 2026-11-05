package gg.optic.client.mixin;

import gg.optic.client.OpticUiDraw;
import gg.optic.client.OpticUiFont;
import gg.optic.client.gui.OfflineSignInScreen;
import gg.optic.client.gui.OpticTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Title-screen play buttons are dark bars. Options, quit, and language are drawn as chrome. */
@Mixin(ButtonWidget.class)
public abstract class ButtonWidgetMixin extends DrawableHelper {
	private static final float LABEL_SIZE = 9.0F;
	private static final int ACCENT = OpticTheme.ACCENT;

	@Shadow public int x;
	@Shadow public int y;
	@Shadow public int id;
	@Shadow public java.lang.String message;
	@Shadow public boolean active;
	@Shadow public boolean visible;
	@Shadow protected boolean hovered;
	@Shadow protected int width;
	@Shadow protected int height;

	@Inject(method = "render(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("HEAD"), cancellable = true)
	private void optic$titleButtons(MinecraftClient client, int mouseX, int mouseY, CallbackInfo ci) {
		boolean title = client != null && client.currentScreen instanceof TitleScreen;
		boolean account = client != null && client.currentScreen instanceof OfflineSignInScreen;
		boolean browser = client != null
			&& (client.currentScreen instanceof MultiplayerScreen || client.currentScreen instanceof SelectWorldScreen);
		boolean pause = client != null && client.currentScreen instanceof GameMenuScreen;
		// Everything except the title list and Optic's account screen uses vanilla drawing with the
		// box style below (see optic$box), so all Minecraft menus and the pause menu match.
		if (!title && !account) return;
		if (!visible) {
			ci.cancel();
			return;
		}

		hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
		boolean menuButton = id == 1 || id == 2 || id == 11 || id == 12 || id == 0 || id == 4 || id == 900;
		if (title && !menuButton) {
			ci.cancel();
			return;
		}
		if (title) {
			String label = message == null ? "" : message;
			if (id == 1) label = "Singleplayer";
			else if (id == 2) label = "Multiplayer";
			else if (id == 0) label = "Options";
			else if (id == 4) label = "Quit";
			else if (id == 900) label = "Mods";
			gg.optic.client.gui.OpticMenuChrome.drawMenuItem(client, x, y, width, height, label.toUpperCase(), hovered && active, active);
			ci.cancel();
			return;
		}

		// Frosted glass button (OpticTheme): accent edge + outline on hover, muted when disabled.
		boolean hot = hovered && active;
		int fill = !active ? OpticTheme.BUTTON_OFF : hot ? OpticTheme.BUTTON_HOVER : OpticTheme.BUTTON;
		OpticUiDraw.fillExact(client, x, y, width, height, 5, fill);
		OpticUiDraw.outlineExact(client, x, y, width, height, 5, hot ? ACCENT : OpticTheme.BORDER);

		String label = message == null ? "" : message;
		if (title && id == 1) label = "Singleplayer";
		else if (title && id == 2) label = "Multiplayer";
		else if (title && id == 0) label = "Options";
		float size = browser || pause ? 8.0F : LABEL_SIZE;
		if (!browser && !pause) label = label.toUpperCase();
		if (!label.isEmpty()) {
			int color = !active ? OpticTheme.FAINT : hot ? ACCENT : OpticTheme.TEXT;
			int textW = OpticUiFont.widthSharp(client, label, color, size);
			int textH = OpticUiFont.heightSharp(client, label, color, size);
			OpticUiFont.drawSharp(client, label, x + (width - textW) / 2, y + (height - textH) / 2, color, size);
		}

		ci.cancel();
	}

	/** Vanilla button background (left half): draw our box over the full button instead. */
	@org.spongepowered.asm.mixin.injection.Redirect(
		method = "render(Lnet/minecraft/client/MinecraftClient;II)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/ButtonWidget;drawTexture(IIIIII)V", ordinal = 0),
		require = 0
	)
	private void optic$box(ButtonWidget self, int bx, int by, int u, int v, int w, int h) {
		// Not from the texture row: sliders always ask for the "disabled" row as their track.
		int state = !active ? 0 : hovered ? 2 : 1;
		gg.optic.client.gui.OpticButtonStyle.box(MinecraftClient.getInstance(), x, y, width, height, state);
	}

	/** Right half of the vanilla texture: already covered by the box. */
	@org.spongepowered.asm.mixin.injection.Redirect(
		method = "render(Lnet/minecraft/client/MinecraftClient;II)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/ButtonWidget;drawTexture(IIIIII)V", ordinal = 1),
		require = 0
	)
	private void optic$boxRight(ButtonWidget self, int bx, int by, int u, int v, int w, int h) {
	}
}