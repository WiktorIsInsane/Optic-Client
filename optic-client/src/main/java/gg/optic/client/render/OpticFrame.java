package gg.optic.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

/**
 * Turns the camera on the frame the mouse moved. Positions stay interpolated,
 * so movement does not step. Nothing here is sent.
 */
public final class OpticFrame {
	private OpticFrame() {
	}

	/** Mouse that arrived after the tick, applied before this frame is drawn. */
	public static void look(MinecraftClient client) {
		if (client == null || client.player == null || client.options == null || client.mouse == null) return;
		if (client.world == null || client.currentScreen != null || client.isPaused() || !client.focused) return;
		GameOptions options = client.options;
		if (options.smoothCameraEnabled) return;
		client.mouse.updateMouse();
		float sensitivity = options.sensitivity * 0.6F + 0.2F;
		float scaled = sensitivity * sensitivity * sensitivity * 8.0F;
		float yaw = client.mouse.x * scaled;
		float pitch = client.mouse.y * scaled;
		if (options.invertYMouse) pitch = -pitch;
		if (yaw == 0.0F && pitch == 0.0F) return;
		client.player.increaseTransforms(yaw, pitch);
	}
}
