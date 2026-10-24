package gg.optic.client.render;

/** Thread-local chat background opacity while ChatHud.render is running. */
public final class OpticChatRender {
	/** -1 = inactive; 0–100 = active opacity percent. */
	public static float backgroundOpacity = -1.0F;
	public static boolean reentry;

	private OpticChatRender() {
	}
}
