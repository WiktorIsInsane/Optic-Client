package gg.optic.client.mixin;

import gg.optic.client.OpticNet;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turn Nagle off when the socket opens. 1.7.10 explicitly turns it on. Setting it on the first swing races
 * the write, and that first hit can sit in the TCP buffer.
 */
@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
	@Inject(method = "channelActive", at = @At("TAIL"))
	private void optic$fastSocket(ChannelHandlerContext ctx, CallbackInfo ci) {
		if (ctx != null) OpticNet.arm(ctx.channel());
	}
}
