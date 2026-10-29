package gg.optic.client.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticHudLayout;
import gg.optic.client.OpticUiFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

/** FPS, CPS, ping, and coordinate readouts drawn the Optic way. */
public final class OpticHud {
	private OpticHud() {
	}

	public static final class Piece {
		public final String text;
		public final int rgb;

		public Piece(String text, int rgb) {
			this.text = text;
			this.rgb = rgb & 0xFFFFFF;
		}
	}

	public static void drawFps(MinecraftClient client, OpticConfig c) {
		drawPieces(client, fpsPieces(c), c.fpsX, c.fpsY, c.fpsOpticFont, c.fpsTextShadow, c.fpsScale);
	}

	public static void drawCps(MinecraftClient client, OpticConfig c) {
		drawPieces(client, cpsPieces(c), c.cpsX, c.cpsY, c.cpsOpticFont, c.cpsTextShadow, c.cpsScale);
	}

	public static void drawPing(MinecraftClient client, OpticConfig c) {
		if (client.player == null) return;
		int ping = OpticPing.display(client);
		if (c.pingBackground) {
			int[] size = measure(client, pingPieces(c, ping), c.pingOpticFont, c.pingScale);
			fill(c.pingX - 2, c.pingY - 2, size[0] + 4, size[1] + 4, withAlpha(c.pingBackgroundColor, 0x90));
		}
		drawPieces(client, pingPieces(c, ping), c.pingX, c.pingY, c.pingOpticFont, c.pingTextShadow, c.pingScale);
	}

	public static boolean drawCoordinates(MinecraftClient client, OpticConfig c) {
		if (!c.coordinatesShowWhileTyping && client.currentScreen instanceof ChatScreen) return false;
		if (client.player == null) return false;
		List<List<Piece>> rows = coordinateRows(client, c, false);
		drawRows(client, c, rows, c.coordinatesX, c.coordinatesY);
		return true;
	}

	public static String fpsPlain(OpticConfig c) {
		return join(fpsPieces(c));
	}

	public static String cpsPlain(OpticConfig c) {
		return join(cpsPieces(c));
	}

	public static String pingPlain(MinecraftClient client, OpticConfig c) {
		return join(pingPieces(c, client == null ? 0 : OpticPing.display(client)));
	}

	public static String coordinatesPlain(OpticConfig c) {
		StringBuilder sb = new StringBuilder();
		List<List<Piece>> rows = coordinateRows(null, c, true);
		for (int i = 0; i < rows.size(); i++) {
			if (i > 0) sb.append('\n');
			sb.append(join(rows.get(i)));
		}
		return sb.toString();
	}

	public static int[] fpsSize(MinecraftClient client, OpticConfig c) {
		return measure(client, fpsPieces(c), c.fpsOpticFont, c.fpsScale);
	}

	public static int[] cpsSize(MinecraftClient client, OpticConfig c) {
		return measure(client, cpsPieces(c), c.cpsOpticFont, c.cpsScale);
	}

	public static int[] pingSize(MinecraftClient client, OpticConfig c) {
		return measure(client, pingPieces(c, client == null ? 0 : OpticPing.display(client)), c.pingOpticFont, c.pingScale);
	}

	public static int[] coordinatesSize(MinecraftClient client, OpticConfig c) {
		return rowsSize(client, coordinateRows(client, c, true), c.coordinatesOpticFont, c.coordinatesScale, c.coordinatesBackground);
	}

	public static List<Piece> fpsPieces(OpticConfig c) {
		int value = MinecraftClient.getCurrentFps();
		List<Piece> pieces = new ArrayList<Piece>();
		bracket(pieces, c.fpsBrackets, true, c.fpsColor);
		if (c.fpsShowLabel && c.fpsReverseOrder) {
			pieces.add(new Piece(Integer.toString(value), c.fpsTextColor));
			pieces.add(new Piece(" FPS", c.fpsColor));
		} else if (c.fpsShowLabel) {
			pieces.add(new Piece("FPS ", c.fpsColor));
			pieces.add(new Piece(Integer.toString(value), c.fpsTextColor));
		} else {
			pieces.add(new Piece(Integer.toString(value), c.fpsTextColor));
		}
		bracket(pieces, c.fpsBrackets, false, c.fpsColor);
		return pieces;
	}

	public static List<Piece> cpsPieces(OpticConfig c) {
		int left = OpticClient.get().clicks().leftCps();
		int right = OpticClient.get().clicks().rightCps();
		List<Piece> body = new ArrayList<Piece>();
		if (c.cpsSeparateButtons) {
			Piece leftPiece = new Piece(Integer.toString(left), c.cpsTextColor);
			Piece bar = new Piece(" | ", c.cpsColor);
			Piece rightPiece = new Piece(Integer.toString(right), c.cpsTextColor);
			if (c.cpsReverseText) {
				body.add(rightPiece);
				body.add(bar);
				body.add(leftPiece);
			} else {
				body.add(leftPiece);
				body.add(bar);
				body.add(rightPiece);
			}
		} else {
			body.add(new Piece(Integer.toString(left + right), c.cpsTextColor));
		}
		List<Piece> pieces = new ArrayList<Piece>();
		bracket(pieces, c.cpsBrackets, true, c.cpsColor);
		if (c.cpsShowLabel && c.cpsReverseText) {
			addAll(pieces, body);
			pieces.add(new Piece(" CPS", c.cpsColor));
		} else if (c.cpsShowLabel) {
			pieces.add(new Piece("CPS ", c.cpsColor));
			addAll(pieces, body);
		} else {
			addAll(pieces, body);
		}
		bracket(pieces, c.cpsBrackets, false, c.cpsColor);
		return pieces;
	}

	public static List<Piece> pingPieces(OpticConfig c, int ping) {
		List<Piece> pieces = new ArrayList<Piece>();
		int numberColor = c.pingDynamicColor ? OpticPing.colorFor(c, ping) : c.pingTextColor;
		String number = ping < 0 ? "..." : Integer.toString(ping);
		if (c.pingShowMs && ping >= 0) number = number + "ms";
		bracket(pieces, c.pingBrackets, true, c.pingColor);
		if (c.pingShowLabel && c.pingReversePrefix) {
			pieces.add(new Piece(number, numberColor));
			pieces.add(new Piece(" " + c.pingPrefix, c.pingColor));
		} else if (c.pingShowLabel) {
			pieces.add(new Piece(c.pingPrefix + " ", c.pingColor));
			pieces.add(new Piece(number, numberColor));
		} else {
			pieces.add(new Piece(number, numberColor));
		}
		String spike = OpticPing.spikeText();
		if (spike.length() > 0) pieces.add(new Piece(spike, c.pingSpikeColor));
		bracket(pieces, c.pingBrackets, false, c.pingColor);
		return pieces;
	}

	public static List<List<Piece>> coordinateRows(MinecraftClient client, OpticConfig c, boolean preview) {
		double x = 0.0;
		double y = 64.0;
		double z = 0.0;
		float yaw = 0.0F;
		String biome = "Plains";
		if (!preview && client != null && client.player != null) {
			ClientPlayerEntity player = client.player;
			x = player.x;
			y = player.getBoundingBox().minY;
			z = player.z;
			yaw = player.yaw;
			if (c.coordinatesBiome && client.world != null) {
				BlockPos pos = new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
				Biome found = client.world.getBiome(pos);
				if (found != null && found.name != null && found.name.length() > 0) biome = found.name;
			}
		}
		String format = "%." + clampDecimals(c.coordinatesDecimals) + "f";
		List<Piece> xRow = axis(c, "X", String.format(format, x));
		List<Piece> yRow = axis(c, "Y", String.format(format, y));
		List<Piece> zRow = axis(c, "Z", String.format(format, z));
		String facing = facing(yaw);
		if (c.coordinatesDirectionMarks) {
			if (facing.indexOf('W') >= 0) append(xRow, " (-)", c.coordinatesColor);
			else if (facing.indexOf('E') >= 0) append(xRow, " (+)", c.coordinatesColor);
			if (facing.indexOf('N') >= 0) append(zRow, " (-)", c.coordinatesColor);
			else if (facing.indexOf('S') >= 0) append(zRow, " (+)", c.coordinatesColor);
		}
		List<List<Piece>> rows = new ArrayList<List<Piece>>();
		if (c.coordinatesHorizontal) {
			List<Piece> line = new ArrayList<Piece>();
			if (c.coordinatesBrackets) line.add(new Piece("[", c.coordinatesColor));
			addAll(line, xRow);
			if (c.coordinatesShowY) {
				line.add(new Piece(", ", c.coordinatesTextColor));
				addAll(line, yRow);
			}
			line.add(new Piece(", ", c.coordinatesTextColor));
			addAll(line, zRow);
			if (c.coordinatesDirection) {
				line.add(new Piece(" " + facing, c.coordinatesColor));
			}
			if (c.coordinatesBiome) {
				line.add(new Piece(" " + biome, c.coordinatesTextColor));
			}
			if (c.coordinatesBrackets) line.add(new Piece("]", c.coordinatesColor));
			rows.add(line);
		} else {
			rows.add(xRow);
			if (c.coordinatesShowY) rows.add(yRow);
			rows.add(zRow);
			if (c.coordinatesDirection) {
				List<Piece> dir = new ArrayList<Piece>();
				dir.add(new Piece(facing, c.coordinatesColor));
				rows.add(dir);
			}
			if (c.coordinatesBiome) {
				List<Piece> bio = new ArrayList<Piece>();
				if (c.coordinatesShowLabel) bio.add(new Piece("Biome ", c.coordinatesColor));
				bio.add(new Piece(biome, c.coordinatesTextColor));
				rows.add(bio);
			}
		}
		return rows;
	}

	private static boolean copyWasDown;

	public static void tickCopy(MinecraftClient client) {
		OpticConfig c = OpticClient.get().config();
		if (!c.coordinates || client.player == null || client.currentScreen != null) {
			copyWasDown = false;
			return;
		}
		boolean down = gg.optic.client.OpticFreeLook.keyDown(c.coordinatesCopyKey);
		if (down && !copyWasDown) {
			String text = String.format(
				"X: %d Y: %d Z: %d",
				(int) client.player.x,
				(int) client.player.y,
				(int) client.player.z
			);
			try {
				java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(text);
				java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
				if (client.inGameHud != null) client.inGameHud.setOverlayMessage("Copied coordinates", false);
			} catch (Exception ignored) {
			}
		}
		copyWasDown = down;
	}

	private static void drawRows(MinecraftClient client, OpticConfig c, List<List<Piece>> rows, int x, int y) {
		int[] size = rowsSize(client, rows, c.coordinatesOpticFont, c.coordinatesScale, false);
		int pad = c.coordinatesBackground ? 3 : 0;
		if (c.coordinatesBackground) {
			fill(x - pad, y - pad, size[0] + pad * 2, size[1] + pad * 2, withAlpha(c.coordinatesBackgroundColor, 0x90));
			if (c.coordinatesBorder) {
				int argb = withAlpha(c.coordinatesBorderColor, 0xCC);
				fill(x - pad, y - pad, size[0] + pad * 2, 1, argb);
				fill(x - pad, y - pad + size[1] + pad * 2 - 1, size[0] + pad * 2, 1, argb);
				fill(x - pad, y - pad, 1, size[1] + pad * 2, argb);
				fill(x - pad + size[0] + pad * 2 - 1, y - pad, 1, size[1] + pad * 2, argb);
			}
		}
		float line = OpticHudLayout.fontSize(c.coordinatesScale) + 2.0F;
		for (int i = 0; i < rows.size(); i++) {
			drawPieces(client, rows.get(i), x, y + Math.round(i * line), c.coordinatesOpticFont, c.coordinatesTextShadow, c.coordinatesScale);
		}
	}

	private static List<Piece> axis(OpticConfig c, String label, String value) {
		List<Piece> pieces = new ArrayList<Piece>();
		if (c.coordinatesShowLabel) pieces.add(new Piece(label + " ", c.coordinatesColor));
		pieces.add(new Piece(value, c.coordinatesTextColor));
		return pieces;
	}

	private static void append(List<Piece> pieces, String text, int rgb) {
		pieces.add(new Piece(text, rgb));
	}

	private static void addAll(List<Piece> into, List<Piece> from) {
		for (int i = 0; i < from.size(); i++) into.add(from.get(i));
	}

	public static String facing(float yaw) {
		String[] names = new String[] { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
		double value = Math.floor(yaw);
		value = value % 360.0;
		if (value < 0.0) value += 360.0;
		value = value + 180.0 + 22.5;
		value = value % 360.0;
		int index = (int) (value / 45.0);
		if (index < 0) index = 0;
		if (index > 7) index = 7;
		return names[index];
	}

	private static int clampDecimals(int decimals) {
		if (decimals < 0) return 0;
		if (decimals > 2) return 2;
		return decimals;
	}

	private static void bracket(List<Piece> pieces, boolean on, boolean open, int rgb) {
		if (!on) return;
		pieces.add(new Piece(open ? "[" : "]", rgb));
	}

	public static void drawPieces(MinecraftClient client, List<Piece> pieces, int x, int y, boolean opticFont, boolean shadow, float scale) {
		int cursor = x;
		float size = OpticHudLayout.fontSize(scale);
		for (int i = 0; i < pieces.size(); i++) {
			Piece piece = pieces.get(i);
			int argb = 0xFF000000 | (piece.rgb & 0xFFFFFF);
			if (opticFont) {
				if (shadow) OpticUiFont.drawSized(client, piece.text, cursor + 1, y + 1, 0xAA000000, size);
				OpticUiFont.drawSized(client, piece.text, cursor, y, argb, size);
				cursor += OpticUiFont.widthSized(client, piece.text, argb, size);
			} else if (Math.abs(scale - 1.0F) > 0.01F) {
				GlStateManager.pushMatrix();
				GlStateManager.translate(cursor, y, 0.0F);
				GlStateManager.scale(scale, scale, 1.0F);
				if (shadow) client.textRenderer.draw(piece.text, 1, 1, 0x000000, true);
				client.textRenderer.draw(piece.text, 0, 0, piece.rgb & 0xFFFFFF, shadow);
				GlStateManager.popMatrix();
				cursor += Math.round(client.textRenderer.getStringWidth(piece.text) * scale);
			} else {
				client.textRenderer.draw(piece.text, cursor, y, piece.rgb & 0xFFFFFF, shadow);
				cursor += client.textRenderer.getStringWidth(piece.text);
			}
		}
	}

	public static int[] measure(MinecraftClient client, List<Piece> pieces, boolean opticFont, float scale) {
		int width = 0;
		float size = OpticHudLayout.fontSize(scale);
		for (int i = 0; i < pieces.size(); i++) {
			Piece piece = pieces.get(i);
			if (opticFont) width += OpticUiFont.widthSized(client, piece.text, 0xFFFFFFFF, size);
			else width += Math.round(client.textRenderer.getStringWidth(piece.text) * scale);
		}
		int height = Math.max(10, Math.round(size + 1.0F));
		return new int[] { Math.max(4, width), height };
	}

	public static int[] rowsSize(MinecraftClient client, List<List<Piece>> rows, boolean opticFont, float scale, boolean pad) {
		int width = 0;
		float line = OpticHudLayout.fontSize(scale) + 2.0F;
		for (int i = 0; i < rows.size(); i++) {
			width = Math.max(width, measure(client, rows.get(i), opticFont, scale)[0]);
		}
		int height = Math.max(10, Math.round(rows.size() * line));
		if (pad) {
			width += 6;
			height += 6;
		}
		return new int[] { width, height };
	}

	public static void fill(int x, int y, int w, int h, int argb) {
		if (w <= 0 || h <= 0) return;
		float a = ((argb >>> 24) & 255) / 255.0F;
		float r = ((argb >> 16) & 255) / 255.0F;
		float g = ((argb >> 8) & 255) / 255.0F;
		float b = (argb & 255) / 255.0F;
		GlStateManager.disableTexture();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buf = tessellator.getBuffer();
		buf.begin(7, VertexFormats.POSITION_COLOR);
		buf.vertex(x, y + h, 0.0).color(r, g, b, a).next();
		buf.vertex(x + w, y + h, 0.0).color(r, g, b, a).next();
		buf.vertex(x + w, y, 0.0).color(r, g, b, a).next();
		buf.vertex(x, y, 0.0).color(r, g, b, a).next();
		tessellator.draw();
		GlStateManager.enableTexture();
		GlStateManager.disableBlend();
	}

	public static int withAlpha(int rgb, int alpha) {
		return (alpha << 24) | (rgb & 0xFFFFFF);
	}

	private static String join(List<Piece> pieces) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < pieces.size(); i++) sb.append(pieces.get(i).text);
		return sb.toString();
	}
}
