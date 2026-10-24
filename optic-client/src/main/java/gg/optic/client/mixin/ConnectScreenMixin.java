package gg.optic.client.mixin;

import gg.optic.client.OpticReconnect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {
	@Inject(
		method = "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerInfo;)V",
		at = @At("RETURN")
	)
	private void optic$rememberServer(Screen parent, MinecraftClient client, ServerInfo server, CallbackInfo ci) {
		OpticReconnect.remember(parent, server);
	}

	@Inject(
		method = "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/MinecraftClient;Ljava/lang/String;I)V",
		at = @At("RETURN")
	)
	private void optic$rememberAddress(Screen parent, MinecraftClient client, String address, int port, CallbackInfo ci) {
		String host = address == null ? "" : address;
		if (port != 25565 && host.indexOf(':') < 0) {
			host = host + ":" + port;
		}
		OpticReconnect.remember(parent, new ServerInfo(host, host, false));
	}
}
