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
		if (!title && !account && !browser && !pause) return;
		if (!visible) {
			ci.cancel();
			return;
		}

		hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
		boolean menuButton = id == 1 || id == 2 || id == 11 || id == 12 || id == 0 || id == 900;
		if (title && !menuButton) {
			ci.cancel();
			return;
		}

		// Frosted glass button (OpticTheme): accent edge + outline on hover, muted when disabled.
		boolean hot = hovered && active;
		int fill = !active ? OpticTheme.BUTTON_OFF : hot ? OpticTheme.BUTTON_HOVER : OpticTheme.BUTTON;
		OpticUiDraw.fillExact(client, x, y, width, height, 5, fill);
		DrawableHelper.fill(x + 5, y + 1, x + width - 5, y + 2, OpticTheme.HIGHLIGHT);
		OpticUiDraw.outlineExact(client, x, y, width, height, 5, hot ? ACCENT : OpticTheme.BORDER);
		if (hot) OpticTheme.accentEdge(x, y, height, true);

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
}
