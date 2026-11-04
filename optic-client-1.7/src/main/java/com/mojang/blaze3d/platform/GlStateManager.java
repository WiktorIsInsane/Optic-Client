package com.mojang.blaze3d.platform;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

/**
 * 1.7.10 has no GlStateManager; vanilla calls OpenGL directly. This stand-in keeps the
 * 1.8.9 method names so the shared Optic drawing code compiles unchanged, and forwards
 * each call straight to OpenGL. There is no state cache, which matches 1.7 itself.
 */
public final class GlStateManager {
	private static Boolean separateBlend;

	private GlStateManager() {
	}

	public static void disableAlphaTest() {
		GL11.glDisable(GL11.GL_ALPHA_TEST);
	}

	public static void enableAlphaTest() {
		GL11.glEnable(GL11.GL_ALPHA_TEST);
	}

	public static void alphaFunc(int func, float ref) {
		GL11.glAlphaFunc(func, ref);
	}

	public static void enableLighting() {
		GL11.glEnable(GL11.GL_LIGHTING);
	}

	public static void disableLighting() {
		GL11.glDisable(GL11.GL_LIGHTING);
	}

	public static void enableColorMaterial() {
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
	}

	public static void disableColorMaterial() {
		GL11.glDisable(GL11.GL_COLOR_MATERIAL);
	}

	public static void disableDepthTest() {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
	}

	public static void enableDepthTest() {
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}

	public static void depthFunc(int func) {
		GL11.glDepthFunc(func);
	}

	public static void depthMask(boolean mask) {
		GL11.glDepthMask(mask);
	}

	public static void disableBlend() {
		GL11.glDisable(GL11.GL_BLEND);
	}

	public static void enableBlend() {
		GL11.glEnable(GL11.GL_BLEND);
	}

	public static void blendFunc(int src, int dst) {
		GL11.glBlendFunc(src, dst);
	}

	public static void blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
		if (separateBlend == null) {
			try {
				separateBlend = Boolean.valueOf(GLContext.getCapabilities().OpenGL14);
			} catch (Throwable t) {
				separateBlend = Boolean.FALSE;
			}
		}
		if (separateBlend.booleanValue()) GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
		else GL11.glBlendFunc(srcRgb, dstRgb);
	}

	public static void enableCull() {
		GL11.glEnable(GL11.GL_CULL_FACE);
	}

	public static void disableCull() {
		GL11.glDisable(GL11.GL_CULL_FACE);
	}

	public static void cullFace(int mode) {
		GL11.glCullFace(mode);
	}

	public static void polygonOffset(float factor, float units) {
		GL11.glPolygonOffset(factor, units);
	}

	public static void enableTexture() {
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	public static void disableTexture() {
		GL11.glDisable(GL11.GL_TEXTURE_2D);
	}

	public static void bindTexture(int texture) {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
	}

	public static void shadeModel(int mode) {
		GL11.glShadeModel(mode);
	}

	public static void enableRescaleNormal() {
		GL11.glEnable(0x803A);
	}

	public static void disableRescaleNormal() {
		GL11.glDisable(0x803A);
	}

	public static void viewport(int x, int y, int width, int height) {
		GL11.glViewport(x, y, width, height);
	}

	public static void colorMask(boolean r, boolean g, boolean b, boolean a) {
		GL11.glColorMask(r, g, b, a);
	}

	public static void clearColor(float r, float g, float b, float a) {
		GL11.glClearColor(r, g, b, a);
	}

	public static void clear(int mask) {
		GL11.glClear(mask);
	}

	public static void matrixMode(int mode) {
		GL11.glMatrixMode(mode);
	}

	public static void loadIdentity() {
		GL11.glLoadIdentity();
	}

	public static void pushMatrix() {
		GL11.glPushMatrix();
	}

	public static void popMatrix() {
		GL11.glPopMatrix();
	}

	public static void ortho(double left, double right, double bottom, double top, double near, double far) {
		GL11.glOrtho(left, right, bottom, top, near, far);
	}

	public static void rotate(float angle, float x, float y, float z) {
		GL11.glRotatef(angle, x, y, z);
	}

	public static void scale(float x, float y, float z) {
		GL11.glScalef(x, y, z);
	}

	public static void scale(double x, double y, double z) {
		GL11.glScaled(x, y, z);
	}

	public static void translate(float x, float y, float z) {
		GL11.glTranslatef(x, y, z);
	}

	public static void translate(double x, double y, double z) {
		GL11.glTranslated(x, y, z);
	}

	public static void color(float r, float g, float b, float a) {
		GL11.glColor4f(r, g, b, a);
	}

	public static void color(float r, float g, float b) {
		GL11.glColor4f(r, g, b, 1.0F);
	}
}
