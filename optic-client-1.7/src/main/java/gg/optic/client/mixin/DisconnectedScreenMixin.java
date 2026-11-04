package gg.optic.client.mixin;

import gg.optic.client.OpticReconnect;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
	@Shadow @Final private Screen parent;

	@Inject(method = "init", at = @At("TAIL"))
	private void optic$reconnectButton(CallbackInfo ci) {
		if (!OpticReconnect.canReconnect()) return;

		// 1.7 puts "Back to title screen" at height / 4 + 132; sit just above it.
		int menuY = this.height / 4 + 132;
		this.buttons.add(new ButtonWidget(9001, this.width / 2 - 100, menuY - 24, "Reconnect"));
	}

	@Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
	private void optic$onReconnect(ButtonWidget button, CallbackInfo ci) {
		if (button == null || button.id != 9001) return;
		ci.cancel();
		ServerInfo server = OpticReconnect.lastServer();
		if (server == null) return;
		Screen back = OpticReconnect.lastParent();
		if (back == null) back = this.parent;
		this.client.setScreen(new ConnectScreen(back, this.client, server));
	}
}
