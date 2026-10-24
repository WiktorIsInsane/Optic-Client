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

	private OpticFreeLook() {
	}

	public static boolean isActive() {
		return engaged;
	}

	/** Call at the start of a frame, before the mouse is applied to the player. */
	public static void beforeRender(MinecraftClient client) {
		OpticConfig config = OpticClient.get().config();
		if (client == null || client.player == null || client.options == null || !config.freeLook || client.currentScreen != null) {
			toggled = false;
			wasDown = false;
			stop(client);
			return;
		}
		boolean down = keyDown(config.freeLookKeyCode);
		if (config.freeLookToggle) {
			if (down && !wasDown) toggled = !toggled;
			if (toggled) start(client);
			else stop(client);
		} else if (down) {
			start(client);
		} else {
			stop(client);
		}
		wasDown = down;
	}

	/**
	 * @return true when the mouse delta was applied to the camera and must not touch the player
	 */
	public static boolean applyLook(float yawDelta, float pitchDelta) {
		if (!engaged) return false;
		camYaw += yawDelta * 0.15F;
		camPitch -= pitchDelta * 0.15F;
		if (camPitch > 90.0F) camPitch = 90.0F;
		if (camPitch < -90.0F) camPitch = -90.0F;
		return true;
	}

	public static float yaw(Entity entity, float actual) {
		if (!useCamera(entity)) return actual;
		return camYaw;
	}

	public static float pitch(Entity entity, float actual) {
		if (!useCamera(entity)) return actual;
		return camPitch;
	}

	public static float distance() {
		float value = OpticClient.get().config().freeLookDistance;
		if (value < 1.5F) return 1.5F;
		if (value > 12.0F) return 12.0F;
		return value;
	}

	/** @return true when the wheel changed the camera distance and hotbar scrolling should wait */
	public static boolean handleScroll(int wheel) {
		if (!engaged || wheel == 0) return false;
		OpticConfig config = OpticClient.get().config();
		float step = wheel > 0 ? -0.5F : 0.5F;
		float next = config.freeLookDistance + step;
		if (next < 1.5F) next = 1.5F;
		if (next > 12.0F) next = 12.0F;
		config.freeLookDistance = next;
		return true;
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
		return client != null && entity == client.player;
	}

	private static void start(MinecraftClient client) {
		if (engaged) return;
		engaged = true;
		camYaw = client.player.yaw;
		camPitch = client.player.pitch;
		savedPerspective = client.options.perspective;
		client.options.perspective = 1;
	}

	private static void stop(MinecraftClient client) {
		if (!engaged) return;
		engaged = false;
		if (client != null && client.options != null) {
			client.options.perspective = savedPerspective;
		}
		OpticClient.get().config().save();
	}

	private static boolean keyDown(int code) {
		if (code < 0) return Mouse.isButtonDown(code + 100);
		if (code == Keyboard.KEY_NONE) return false;
		return Keyboard.isKeyDown(code);
	}
}
