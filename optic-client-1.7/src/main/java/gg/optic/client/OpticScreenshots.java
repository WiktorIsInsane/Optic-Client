package gg.optic.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.ClickEventAction;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.HoverEventAction;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;

/**
 * Screenshot chat buttons: after "Saved screenshot as ..." comes [Open] [Copy] [Folder].
 * Copy goes through a local command that is caught before it is sent to the server.
 */
public final class OpticScreenshots {
	private static final String COMMAND = "/optic-screenshot ";

	private OpticScreenshots() {
	}

	/** Adds the buttons to the message vanilla returns after saving a screenshot. */
	public static Text decorate(Text message) {
		if (message == null) return null;
		String path = findFile(message);
		if (path == null) return message;
		File file = new File(path);
		Text out = message.copy();
		out.append(new LiteralText(" "));
		out.append(button("Open", Formatting.GREEN, new ClickEvent(ClickEventAction.OPEN_FILE, file.getAbsolutePath()), "Open the screenshot"));
		out.append(new LiteralText(" "));
		out.append(button("Copy", Formatting.AQUA, new ClickEvent(ClickEventAction.RUN_COMMAND, COMMAND + "copy " + file.getName()), "Copy the image to your clipboard"));
		out.append(new LiteralText(" "));
		File folder = file.getParentFile();
		if (folder != null) {
			out.append(button("Folder", Formatting.YELLOW, new ClickEvent(ClickEventAction.OPEN_FILE, folder.getAbsolutePath()), "Open the screenshots folder"));
		}
		return out;
	}

	private static Text button(String label, Formatting color, ClickEvent click, String hover) {
		Text text = new LiteralText("[" + label + "]");
		Style style = new Style();
		style.setFormatting(color);
		style.setBold(Boolean.TRUE);
		style.setClickEvent(click);
		style.setHoverEvent(new HoverEvent(HoverEventAction.SHOW_TEXT, new LiteralText(hover)));
		text.setStyle(style);
		return text;
	}

	/** The saved file's path is the OPEN_FILE click on the file name inside vanilla's message. */
	private static String findFile(Text text) {
		ClickEvent click = text.getStyle() == null ? null : text.getStyle().getClickEvent();
		if (click != null && click.getAction() == ClickEventAction.OPEN_FILE) return click.getValue();
		if (text instanceof TranslatableText) {
			for (Object arg : ((TranslatableText) text).getArgs()) {
				if (arg instanceof Text) {
					String found = findFile((Text) arg);
					if (found != null) return found;
				}
			}
		}
		for (Object sibling : text.getSiblings()) {
			if (sibling instanceof Text) {
				String found = findFile((Text) sibling);
				if (found != null) return found;
			}
		}
		return null;
	}

	/** @return true when the chat line was one of our commands (and must not be sent). */
	public static boolean handleCommand(String message) {
		if (message == null || !message.startsWith(COMMAND)) return false;
		String rest = message.substring(COMMAND.length()).trim();
		if (rest.startsWith("copy ")) {
			String name = rest.substring(5).trim();
			MinecraftClient client = MinecraftClient.getInstance();
			File dir = new File(client.runDirectory, "screenshots");
			File file = new File(dir, name);
			// Only plain file names inside the screenshots folder.
			if (name.contains("/") || name.contains("\\") || name.contains("..") || !file.isFile()) {
				say("Screenshot not found.", Formatting.RED);
				return true;
			}
			copyImage(file);
		}
		return true;
	}

	private static void copyImage(final File file) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final Image image = ImageIO.read(file);
					if (image == null) throw new IllegalStateException("unreadable image");
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
						@Override
						public DataFlavor[] getTransferDataFlavors() {
							return new DataFlavor[] { DataFlavor.imageFlavor };
						}

						@Override
						public boolean isDataFlavorSupported(DataFlavor flavor) {
							return DataFlavor.imageFlavor.equals(flavor);
						}

						@Override
						public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
							if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
							return image;
						}
					}, null);
					say("Copied screenshot to clipboard.", Formatting.GREEN);
				} catch (Throwable error) {
					say("Couldn't copy the screenshot: " + error.getMessage(), Formatting.RED);
				}
			}
		}, "Optic screenshot copy");
		thread.setDaemon(true);
		thread.start();
	}

	private static void say(final String message, final Formatting color) {
		final MinecraftClient client = MinecraftClient.getInstance();
		client.execute(new java.util.concurrent.Callable<Object>() {
			@Override
			public Object call() {
				if (client.inGameHud == null || client.inGameHud.getChatHud() == null) return null;
				Text text = new LiteralText(message);
				Style style = new Style();
				style.setFormatting(color);
				text.setStyle(style);
				client.inGameHud.getChatHud().addMessage(text);
				return null;
			}
		});
	}
}
