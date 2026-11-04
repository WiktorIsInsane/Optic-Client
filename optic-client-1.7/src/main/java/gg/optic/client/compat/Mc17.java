package gg.optic.client.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import org.lwjgl.opengl.GL11;

/** Small 1.7.10 stand-ins for 1.8.9 calls the shared Optic code makes. */
public final class Mc17 {
	private Mc17() {
	}

	/** 1.8 has gg.optic.client.compat.Mc17.fps(); 1.7 keeps it private, but the F3 string starts with it. */
	public static int fps() {
		MinecraftClient client = MinecraftClient.getInstance();
		String text = client == null ? null : client.fpsDebugString;
		if (text == null) return 0;
		int end = 0;
		while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
		if (end == 0) return 0;
		try {
			return Integer.parseInt(text.substring(0, end));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}

	/**
	 * 1.8 AbstractTexture.setFilter. The mipmap flag is ignored: these textures have no
	 * mip levels, and a mipmap filter on them would draw nothing.
	 */
	public static void setFilter(AbstractTexture texture, boolean blur, boolean mipmap) {
		if (texture == null) return;
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlId());
		int filter = blur ? GL11.GL_LINEAR : GL11.GL_NEAREST;
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
	}
}
