package gg.optic.client;

import gg.optic.client.mixin.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;

import java.util.List;

/**
 * Settings > General > Copy Chat Messages: with chat open, Ctrl+C over a message copies the
 * whole message (all its wrapped lines) as the plain text you see.
 */
public final class OpticChatCopy {
	private OpticChatCopy() {
	}

	/** @return true when a message was under the mouse and got copied. */
	public static boolean copyHovered(MinecraftClient client) {
		if (client == null || client.inGameHud == null) return log("no HUD");
		ChatHud chat = client.inGameHud.getChatHud();
		if (chat == null || !chat.isChatFocused()) return log("chat not open");
		ChatHudAccessor access = (ChatHudAccessor) chat;
		List<?> lines = access.optic$visibleMessages();
		if (lines == null || lines.isEmpty()) return log("no chat lines");

		ChatHudLine line = null;
		// 1) Ask vanilla which text piece is under the mouse (what chat clicks use), then find
		//    the drawn line that piece belongs to.
		net.minecraft.text.Text piece = chat.getTextAt(Mouse.getX(), Mouse.getY());
		if (piece != null) {
			for (Object item : lines) {
				ChatHudLine candidate = (ChatHudLine) item;
				for (Object part : candidate.getText()) {
					if (part == piece) {
						line = candidate;
						break;
					}
				}
				if (line != null) break;
			}
		}
		// 2) Otherwise the same row math vanilla uses (works over the empty part of a row too).
		if (line == null) {
			int scale = new Window(client, client.width, client.height).getScaleFactor();
			float chatScale = chat.getChatScale();
			int x = MathHelper.floor((Mouse.getX() / scale - 3) / chatScale);
			int y = MathHelper.floor((Mouse.getY() / scale - 27) / chatScale);
			int shown = Math.min(chat.getVisibleLineCount(), lines.size());
			int fontHeight = client.textRenderer.fontHeight;
			if (x < 0 || y < 0 || x > MathHelper.floor(chat.getWidth() / chatScale) || y >= fontHeight * shown + shown) {
				return log("mouse not over chat (x=" + x + " y=" + y + ")");
			}
			int index = y / fontHeight + access.optic$scrolledLines();
			if (index < 0 || index >= lines.size()) return log("row " + index + " out of range");
			line = (ChatHudLine) lines.get(index);
		}

		String text = null;
		ChatHudLine message = messageFor(lines, access.optic$messages(), indexOf(lines, line));
		if (message != null) text = message.getText().asUnformattedString();
		if (text == null) text = line.getText().asUnformattedString();
		text = net.minecraft.util.Formatting.strip(text);
		if (text == null || text.isEmpty()) return log("message is empty");
		Screen.setClipboard(text);
		client.inGameHud.setOverlayMessage("Copied message to clipboard", false);
		System.out.println("[Optic] Copy chat: copied " + text.length() + " characters");
		return true;
	}

	private static int indexOf(List<?> lines, Object line) {
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i) == line) return i;
		}
		return -1;
	}

	/**
	 * The full message a drawn line came from. Both lists are newest first, and each message's
	 * wrapped lines sit together in the drawn list, so walk them in step: a message owns as many
	 * drawn lines as it takes to cover its text. Matching on the arrival tick alone failed for
	 * servers, which often send several messages (and blank spacer lines) in the same tick.
	 */
	private static ChatHudLine messageFor(List<?> lines, List<?> messages, int lineIndex) {
		if (lineIndex < 0 || messages == null) return null;
		int next = 0;
		for (Object item : messages) {
			ChatHudLine message = (ChatHudLine) item;
			int want = squash(message.getText().asUnformattedString()).length();
			int start = next;
			int have = 0;
			do {
				if (next >= lines.size()) break;
				have += squash(((ChatHudLine) lines.get(next)).getText().asUnformattedString()).length();
				next++;
			} while (have < want);
			if (lineIndex >= start && lineIndex < next) return message;
			if (next >= lines.size()) break;
		}
		return null;
	}

	/** Visible characters only: no color codes, no spaces or line breaks (wrapping moves those). */
	private static String squash(String text) {
		String plain = net.minecraft.util.Formatting.strip(text);
		return plain == null ? "" : plain.replaceAll("\\s+", "");
	}

	/** Diagnostic line in the game log for each Ctrl+C that didn't copy. */
	private static boolean log(String reason) {
		System.out.println("[Optic] Copy chat: nothing copied, " + reason);
		return false;
	}
}
