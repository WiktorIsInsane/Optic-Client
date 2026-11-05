package gg.optic.client.render;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

/**
 * Motion Blur for 1.7.10, which has no post-processing shaders. After the world is drawn each
 * frame, the previous (already blurred) frame is laid over it at the chosen strength, and the
 * result is copied out to be next frame's "previous". The HUD is drawn afterwards, so it stays sharp.
 */
public final class OpticMotionBlur {
	public static final String[] TYPES = { "V1", "V2", "V3" };

	private static int texture = -1;
	private static int texW;
	private static int texH;
	private static boolean hasFrame;

	private OpticMotionBlur() {
	}

	/**
	 * How much of the previous frame stays, per type. V1: heavy trails that grow with Amount;
	 * V2: inverted (low Amount = heavier); V3: Amount / 10 (the default).
	 */
	static float strength(OpticConfig c) {
		float amount = Math.max(1.0F, Math.min(10.0F, c.motionBlurAmount));
		float value;
		switch (c.motionBlurType) {
			case 0: value = 0.7F + amount / 100.0F * 3.0F - 0.01F; break;
			case 1: value = 1.0F - Math.min(0.99F, amount / 10.0F); break;
			default: value = amount / 10.0F; break;
		}
		// Never quite 1: the picture would freeze.
		return Math.max(0.0F, Math.min(0.95F, value));
	}

	/** Called right after the world (and hand) rendered, before the HUD. */
	public static void afterWorld(MinecraftClient client) {
		OpticClient optic = OpticClient.get();
		OpticConfig c = optic == null ? null : optic.config();
		if (c == null || !c.motionBlur || client.world == null) {
			release();
			return;
		}
		int w = client.width;
		int h = client.height;
		if (w <= 0 || h <= 0) return;
		if (texture == -1 || w != texW || h != texH) create(w, h);

		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		GL11.glOrtho(0.0D, w, h, 0.0D, -1.0D, 1.0D);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		try {
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
			if (hasFrame) {
				GL11.glDisable(GL11.GL_DEPTH_TEST);
				GL11.glDepthMask(false);
				GL11.glDisable(GL11.GL_LIGHTING);
				GL11.glDisable(GL11.GL_FOG);
				GL11.glDisable(GL11.GL_ALPHA_TEST);
				GL11.glDisable(GL11.GL_CULL_FACE);
				GL11.glEnable(GL11.GL_TEXTURE_2D);
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GL11.glColor4f(1.0F, 1.0F, 1.0F, strength(c));
				// Copied textures are bottom-up, the GUI-style ortho is top-down: flip v.
				GL11.glBegin(GL11.GL_QUADS);
				GL11.glTexCoord2f(0.0F, 1.0F);
				GL11.glVertex2f(0.0F, 0.0F);
				GL11.glTexCoord2f(0.0F, 0.0F);
				GL11.glVertex2f(0.0F, h);
				GL11.glTexCoord2f(1.0F, 0.0F);
				GL11.glVertex2f(w, h);
				GL11.glTexCoord2f(1.0F, 1.0F);
				GL11.glVertex2f(w, 0.0F);
				GL11.glEnd();
			}
			// What is on screen now (blended) becomes the previous frame.
			GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
			hasFrame = true;
		} finally {
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glPopMatrix();
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glPopMatrix();
			GL11.glPopAttrib();
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		}
	}

	private static void create(int w, int h) {
		release();
		texture = GL11.glGenTextures();
		texW = w;
		texH = h;
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, w, h, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		hasFrame = false;
	}

	/** Frees the texture when the mod is off or you leave the world. */
	public static void release() {
		if (texture != -1) {
			GL11.glDeleteTextures(texture);
			texture = -1;
		}
		hasFrame = false;
	}
}
