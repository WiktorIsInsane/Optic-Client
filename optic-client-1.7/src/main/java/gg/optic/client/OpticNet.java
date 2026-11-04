package gg.optic.client;

import gg.optic.client.mixin.OpticConnectionAccess;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import net.minecraft.client.MinecraftClient;

/**
 * Makes the packets Minecraft already sends leave the machine as soon as the
 * tick writes them. Nagle otherwise holds a small swing for up to 200ms, and
 * a reconnect used to skip the option because it was remembered process-wide.
 * No extra packets, no reach change, no off-tick swings.
 */
public final class OpticNet {
	/** IPTOS_LOWDELAY. Routers may ignore it; the socket option is still worth setting. */
	private static final int LOW_DELAY = 0x10;

	private OpticNet() {
	}

	public static void arm(Channel channel) {
		if (channel == null) return;
		apply(channel);
		raiseIo(channel);
	}

	public static void flush(MinecraftClient client) {
		if (client == null || client.getNetworkHandler() == null) return;
		if (client.getNetworkHandler().getClientConnection() == null) return;
		Channel channel = ((OpticConnectionAccess) client.getNetworkHandler().getClientConnection()).optic$channel();
		if (channel == null || !channel.isOpen()) return;
		apply(channel);
		raiseIo(channel);
		try {
			channel.flush();
		} catch (Throwable ignored) {
		}
	}

	/** Nagle off, keepalive, and a low-delay hint. Applied again on every channel. */
	private static void apply(Channel channel) {
		try {
			channel.config().setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
		} catch (Throwable ignored) {
		}
		try {
			channel.config().setOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
		} catch (Throwable ignored) {
		}
		try {
			channel.config().setOption(ChannelOption.IP_TOS, Integer.valueOf(LOW_DELAY));
		} catch (Throwable ignored) {
		}
	}

	private static void raiseIo(final Channel channel) {
		final EventLoop loop = channel.eventLoop();
		if (loop == null) return;
		Runnable raise = new Runnable() {
			@Override
			public void run() {
				Thread io = Thread.currentThread();
				int target = Thread.NORM_PRIORITY + 2;
				if (io.getPriority() < target) io.setPriority(target);
			}
		};
		if (loop.inEventLoop()) raise.run();
		else loop.execute(raise);
	}
}
