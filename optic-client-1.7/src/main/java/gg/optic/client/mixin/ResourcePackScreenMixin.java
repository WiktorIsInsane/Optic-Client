package gg.optic.client.mixin;

import gg.optic.client.OpticUiFont;
import gg.optic.client.gui.OpticTheme;
import net.minecraft.client.gui.screen.ResourcePackScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Adds "Import" to the resource pack screen: copies every pack from the normal .minecraft
 * resourcepacks folder into Optic's, skipping packs that are already there.
 */
@Mixin(ResourcePackScreen.class)
public abstract class ResourcePackScreenMixin extends Screen {
	@Unique private static final int IMPORT_BUTTON = 950;
	@Unique private static String optic$status = "";
	@Unique private static long optic$statusUntil;

	/** [folder] [import]  [ Done ]: the two pack buttons are small square icon buttons. */
	@Inject(method = "init()V", at = @At("TAIL"))
	private void optic$addImport(CallbackInfo ci) {
		@SuppressWarnings("unchecked")
		java.util.List<ButtonWidget> list = (java.util.List<ButtonWidget>) buttons;
		int icon = 20;
		int done = 150;
		int total = icon + 4 + icon + 8 + done;
		int left = width / 2 - total / 2;
		int y = height - 48;
		for (ButtonWidget button : list) {
			if (button.id == 2) {
				optic$size(button, icon);
				button.x = left;
				button.y = y;
				button.message = "";
			} else if (button.id == 1) {
				optic$size(button, done);
				button.x = left + icon + 4 + icon + 8;
				button.y = y;
			}
		}
		list.add(new ButtonWidget(IMPORT_BUTTON, left + icon + 4, y, icon, 20, ""));
	}

	/** Drop the "(Place resource pack files here)" hint under the lists. */
	@org.spongepowered.asm.mixin.injection.Redirect(
		method = "render(IIF)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/ResourcePackScreen;drawCenteredString(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V",
			ordinal = 1
		),
		require = 0
	)
	private void optic$hideFolderInfo(ResourcePackScreen self, net.minecraft.client.font.TextRenderer renderer, String text, int x, int y, int color) {
	}
	@Unique private static net.minecraft.util.Identifier optic$folderIcon;
	@Unique private static net.minecraft.util.Identifier optic$importIcon;

	/** Icons on the two square buttons, plus a label above whichever one is hovered. */
	@Inject(method = "render(IIF)V", at = @At("TAIL"))
	private void optic$icons(int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (optic$folderIcon == null) optic$folderIcon = optic$bake("optic_pack_folder", false);
		if (optic$importIcon == null) optic$importIcon = optic$bake("optic_pack_import", true);
		@SuppressWarnings("unchecked")
		java.util.List<ButtonWidget> list = (java.util.List<ButtonWidget>) buttons;
		for (ButtonWidget button : list) {
			boolean folder = button.id == 2;
			if (!folder && button.id != IMPORT_BUTTON) continue;
			net.minecraft.util.Identifier id = folder ? optic$folderIcon : optic$importIcon;
			int size = 12;
			if (id != null) {
				com.mojang.blaze3d.platform.GlStateManager.enableBlend();
				com.mojang.blaze3d.platform.GlStateManager.blendFuncSeparate(770, 771, 1, 0);
				com.mojang.blaze3d.platform.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				client.getTextureManager().bindTexture(id);
				drawTexture(button.x + (20 - size) / 2, button.y + (20 - size) / 2, 0.0F, 0.0F, 64, 64, size, size, 64.0F, 64.0F);
			}
			boolean hot = mouseX >= button.x && mouseY >= button.y && mouseX < button.x + 20 && mouseY < button.y + 20;
			if (hot) {
				String tip = folder ? "Open pack folder" : "Import packs from .minecraft";
				int tw = OpticUiFont.widthSharp(client, tip, 0xFFFFFFFF, 8.0F);
				int th = OpticUiFont.heightSharp(client, tip, 0xFFFFFFFF, 8.0F);
				int tx = Math.max(2, button.x + 10 - tw / 2 - 4);
				int ty = button.y - th - 10;
				gg.optic.client.OpticUiDraw.fillExact(client, tx, ty, tw + 8, th + 6, 3, 0xE0101014);
				OpticUiFont.drawSharp(client, tip, tx + 4, ty + 3, 0xFFFFFFFF, 8.0F);
			}
		}
	}

	/** 64px white icons: a folder, or a tray with a down arrow (import). */
	@Unique
	private net.minecraft.util.Identifier optic$bake(String name, boolean importIcon) {
		try {
			int s = 64;
			java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(s, s, java.awt.image.BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D g = image.createGraphics();
			g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL, java.awt.RenderingHints.VALUE_STROKE_PURE);
			g.setColor(java.awt.Color.WHITE);
			g.setStroke(new java.awt.BasicStroke(5.0F, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
			if (importIcon) {
				g.drawLine(32, 8, 32, 40);
				g.drawLine(19, 28, 32, 41);
				g.drawLine(45, 28, 32, 41);
				g.drawPolyline(new int[] { 8, 8, 56, 56 }, new int[] { 40, 54, 54, 40 }, 4);
			} else {
				java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
				p.moveTo(8, 16);
				p.lineTo(25, 16);
				p.lineTo(30, 22);
				p.lineTo(56, 22);
				p.lineTo(56, 52);
				p.lineTo(8, 52);
				p.closePath();
				g.draw(p);
				g.drawLine(8, 29, 56, 29);
			}
			g.dispose();
			net.minecraft.client.texture.NativeImageBackedTexture texture = new net.minecraft.client.texture.NativeImageBackedTexture(image);
			net.minecraft.util.Identifier id = client.getTextureManager().registerDynamicTexture(name, texture);
			gg.optic.client.OpticSmooth.linearize(texture);
			return id;
		} catch (Exception e) {
			return null;
		}
	}
	@Unique
	private static void optic$size(ButtonWidget button, int w) {
		((ButtonWidgetAccessor) button).optic$setWidth(w);
	}

	@Inject(method = "buttonClicked(Lnet/minecraft/client/gui/widget/ButtonWidget;)V", at = @At("HEAD"), cancellable = true)
	private void optic$import(ButtonWidget button, CallbackInfo ci) {
		if (button.id != IMPORT_BUTTON) return;
		ci.cancel();
		File target = client.getResourcePackLoader().getResourcePackDir();
		File source = optic$minecraftPacks();
		if (source == null || !source.isDirectory()) {
			optic$show("No .minecraft resourcepacks folder found");
			return;
		}
		File[] packs = source.listFiles();
		int copied = 0;
		int skipped = 0;
		int failed = 0;
		if (packs != null) {
			for (File pack : packs) {
				boolean zip = pack.isFile() && pack.getName().toLowerCase().endsWith(".zip");
				boolean folder = pack.isDirectory() && new File(pack, "pack.mcmeta").isFile();
				if (!zip && !folder) continue;
				File dest = new File(target, pack.getName());
				if (dest.exists()) {
					skipped++;
					continue;
				}
				try {
					optic$copy(pack, dest);
					copied++;
				} catch (IOException e) {
					failed++;
					System.err.println("[Optic] Pack import failed for " + pack.getName() + ": " + e);
				}
			}
		}
		String msg = copied == 0 ? "Nothing new to import" : "Imported " + copied + (copied == 1 ? " pack" : " packs");
		if (skipped > 0) msg += " (" + skipped + " already here)";
		if (failed > 0) msg += ", " + failed + " failed";
		optic$show(msg);
		// Re-open so the new packs show up in the list.
		if (copied > 0) client.setScreen((Screen) (Object) this);
	}

	@Unique
	private static void optic$show(String message) {
		optic$status = message;
		optic$statusUntil = System.currentTimeMillis() + 4000L;
	}

	@Unique
	private static File optic$minecraftPacks() {
		String appData = System.getenv("APPDATA");
		File base = appData != null
			? new File(appData, ".minecraft")
			: new File(System.getProperty("user.home"), ".minecraft");
		return new File(base, "resourcepacks");
	}

	@Unique
	private static void optic$copy(File from, File to) throws IOException {
		if (from.isDirectory()) {
			if (!to.mkdirs() && !to.isDirectory()) throw new IOException("cannot create " + to);
			File[] children = from.listFiles();
			if (children == null) return;
			for (File child : children) optic$copy(child, new File(to, child.getName()));
		} else {
			Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
		}
	}
}