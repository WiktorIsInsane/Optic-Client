package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Chat module's Background Opacity also applies to the bar you type in. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
	@Redirect(
		method = "render(IIF)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ChatScreen;fill(IIIII)V"),
		require = 0
	)
	private void optic$inputBackground(int x1, int y1, int x2, int y2, int color) {
		OpticConfig c = OpticClient.get().config();
		if (c.chat && (color & 0x00FFFFFF) == 0) {
			float op = Math.max(0.0F, Math.min(100.0F, c.chatBackgroundOpacity)) / 100.0F;
			if (op <= 0.0F) return;
			int a = (color >>> 24) & 0xFF;
			color = Math.min(255, Math.round(a * op)) << 24;
		}
		DrawableHelper.fill(x1, y1, x2, y2, color);
	}
}
