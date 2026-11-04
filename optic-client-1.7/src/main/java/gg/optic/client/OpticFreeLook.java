package gg.optic.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Hold a key to orbit the camera around the player.
 * The player's yaw and pitch stay where they were, so movement, aiming,
 * and the look packets vanilla already sends are unchanged.
 */
public final class OpticFreeLook {
	private static boolean engaged;
	private static boolean toggled;
	private static boolean wasDown;
	private static int savedPerspective;
	private static float camYaw;
	private static float camPitch;
	private static float renderYaw;
	private static float renderPitch;
	private static long lastFrame;
	private static float savedDistance = 4.0F;

	private OpticFreeLook() {
	}

	public static boolean isActive() {
		return engaged;
	}

	/** Call at the start of a frame, before the mouse is applied to the player. */
	public static void beforeRender(MinecraftClient client) {
		OpticConfig config = OpticClient.get().config();
		if (client == null || client.field_3805 == null || client.options == null || !config.freeLook || client.currentScreen != null) {
			toggled = false;
			wasDown = false;
			stop(client);
			return;
		}
		boolean down = keyDown(config.freeLookKeyCode);
		if (config.freeLookToggle) {
			if (down && !wasDown) toggled = !toggled;
			if (toggled) start(client, config);
			else stop(client);
		} else if (down) {
			start(client, config);
		} else {
			stop(client);
		}
		wasDown = down;
		if (engaged) {
			ease(config);
			applyDistance(client, config);
			int perspective = config.freeLookPerspective == 2 ? 2 : 1;
			client.options.perspective = perspective;
		}
	}

	/**
	 * @return true when the mouse delta was applied to the camera and must not touch the player
	 */
	public static boolean applyLook(float yawDelta, float pitchDelta) {
		if (!engaged) return false;
		OpticConfig config = OpticClient.get().config();
		float yawSign = config.freeLookInvertYaw ? -1.0F : 1.0F;
		float pitchSign = config.freeLookInvertPitch ? 1.0F : -1.0F;
		camYaw += yawDelta * 0.15F * yawSign;
		camPitch += pitchDelta * 0.15F * pitchSign;
		if (camPitch > 90.0F) camPitch = 90.0F;
		if (camPitch < -90.0F) camPitch = -90.0F;
		return true;
	}

	public static float yaw(Entity entity, float actual) {
		if (!useCamera(entity)) return actual;
		if (OpticClient.get().config().freeLookSmooth) return renderYaw;
		return camYaw;
	}

	public static float pitch(Entity entity, float actual) {
		if (!useCamera(entity)) return actual;
		if (OpticClient.get().config().freeLookSmooth) return renderPitch;
		return camPitch;
	}

	public static String keyName(int code) {
		if (code < 0) {
			int button = code + 100;
			if (button == 0) return "Mouse Left";
			if (button == 1) return "Mouse Right";
			if (button == 2) return "Mouse Middle";
			return "Mouse " + (button + 1);
		}
		if (code == Keyboard.KEY_NONE) return "None";
		String name = Keyboard.getKeyName(code);
		if (name == null || name.length() == 0) return "Key " + code;
		return name;
	}

	private static boolean useCamera(Entity entity) {
		if (!engaged || entity == null) return false;
		MinecraftClient client = MinecraftClient.getInstance();
		return client != null && entity == client.field_3805;
	}

	private static void start(MinecraftClient client, OpticConfig config) {
		if (engaged) return;
		engaged = true;
		camYaw = client.field_3805.yaw;
		camPitch = client.field_3805.pitch;
		renderYaw = camYaw;
		renderPitch = camPitch;
		lastFrame = 0L;
		savedPerspective = client.options.perspective;
		client.options.perspective = config.freeLookPerspective == 2 ? 2 : 1;
		if (client.gameRenderer != null) {
			savedDistance = ((gg.optic.client.mixin.OpticGameRendererAccess) client.gameRenderer).optic$getThirdPersonDistance();
		}
	}

	private static void applyDistance(MinecraftClient client, OpticConfig config) {
		if (client.gameRenderer == null) return;
		float distance = Math.max(1.5F, Math.min(12.0F, config.freeLookDistance));
		((gg.optic.client.mixin.OpticGameRendererAccess) client.gameRenderer).optic$setThirdPersonDistance(distance);
		((gg.optic.client.mixin.OpticGameRendererAccess) client.gameRenderer).optic$setLastThirdPersonDistance(distance);
	}

	private static void stop(MinecraftClient client) {
		if (!engaged) return;
		engaged = false;
		if (client != null && client.options != null) {
			client.options.perspective = savedPerspective;
		}
		if (client != null && client.gameRenderer != null) {
			((gg.optic.client.mixin.OpticGameRendererAccess) client.gameRenderer).optic$setThirdPersonDistance(savedDistance);
			((gg.optic.client.mixin.OpticGameRendererAccess) client.gameRenderer).optic$setLastThirdPersonDistance(savedDistance);
		}
	}

	public static boolean keyDown(int code) {
		if (code < 0) return Mouse.isButtonDown(code + 100);
		if (code == Keyboard.KEY_NONE) return false;
		return Keyboard.isKeyDown(code);
	}

	private static void ease(OpticConfig config) {
		long now = System.currentTimeMillis();
		float dt = lastFrame == 0L ? 0.016F : Math.min(0.05F, (now - lastFrame) / 1000.0F);
		lastFrame = now;
		if (!config.freeLookSmooth) {
			renderYaw = camYaw;
			renderPitch = camPitch;
			return;
		}
		float follow = 1.0F - (float) Math.exp(-dt * 10.0F);
		renderYaw = approachAngle(renderYaw, camYaw, follow);
		renderPitch += (camPitch - renderPitch) * follow;
	}

	private static float approachAngle(float from, float to, float amount) {
		float delta = to - from;
		while (delta > 180.0F) delta -= 360.0F;
		while (delta < -180.0F) delta += 360.0F;
		return from + delta * amount;
	}
}
