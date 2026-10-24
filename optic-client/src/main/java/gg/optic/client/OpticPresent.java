package gg.optic.client;

import gg.optic.client.mixin.OpticGameRendererAccess;
import net.minecraft.client.MinecraftClient;

/**
 * In a world, draw straight to the window. The extra framebuffer copy is a
 * full-screen pass and a GPU stall. The title screen still uses the framebuffer
 * because the menu blur samples it. Shaders and spectator outlines do too.
 */
public final class OpticPresent {
	private OpticPresent() {
	}

	public static boolean direct(MinecraftClient client) {
		if (client == null || client.world == null || client.getFramebuffer() == null) return false;
		if (client.gameRenderer instanceof OpticGameRendererAccess
			&& ((OpticGameRendererAccess) client.gameRenderer).optic$shadersEnabled()) {
			return false;
		}
		if (client.player != null
			&& client.player.isSpectator()
			&& client.options != null
			&& client.options.spectatorOutlines.isPressed()) {
			return false;
		}
		return true;
	}
}
