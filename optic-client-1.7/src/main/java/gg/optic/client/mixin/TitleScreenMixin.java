package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticTextures;
import gg.optic.client.gui.AccountMenuOverlay;
import gg.optic.client.gui.OpticMenuBlur;
import gg.optic.client.gui.OpticMenuChrome;
import gg.optic.client.gui.OpticModScreen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.LanguageButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	private static final String COPYRIGHT = "Copyright Mojang AB. Do not distribute!";

	@Shadow
	private String splashText;

	@Unique
	private final AccountMenuOverlay optic$accounts = new AccountMenuOverlay();

	@Inject(method = "init()V", at = @At("TAIL"))
	private void optic$init(CallbackInfo ci) {
		this.splashText = "";
		removeRealmsButton();
		layoutMenu();
		optic$accounts.reload();
		System.out.println("[Optic] Main menu ready — " + OpticClient.VERSION);
		OpticTextures.logo(client);
	}

	@Inject(method = "mouseClicked(III)V", at = @At("HEAD"), cancellable = true)
	private void optic$accountClick(int mouseX, int mouseY, int button, CallbackInfo ci) {
		if (button == 0 && optic$accounts.mouseClicked(client, mouseX, mouseY, width)) {
			ci.cancel();
		}
	}

	@Inject(method = "buttonClicked(Lnet/minecraft/client/gui/widget/ButtonWidget;)V", at = @At("HEAD"), cancellable = true)
	private void optic$modsButton(ButtonWidget button, CallbackInfo ci) {
		if (button.id != MODS_BUTTON) return;
		client.setScreen(new OpticModScreen());
		ci.cancel();
	}

	@Inject(method = "keyPressed(CI)V", at = @At("HEAD"), cancellable = true)
	private void optic$accountKey(char typed, int key, CallbackInfo ci) {
		if (optic$accounts.keyPressed(client, typed, key)) {
			ci.cancel();
		}
	}

	/** Strip Minecraft Realms from the main menu. */
	private void removeRealmsButton() {
		@SuppressWarnings("unchecked")
		java.util.Iterator<ButtonWidget> it = (java.util.Iterator<ButtonWidget>) buttons.iterator();
		while (it.hasNext()) {
			ButtonWidget button = it.next();
			boolean realms = button.id == 14
				|| (button.message != null && button.message.toLowerCase().contains("realms"));
			if (realms) it.remove();
		}
	}

	/** Opens the Optic mod menu from the main menu. */
	@Unique private static final int MODS_BUTTON = 900;

	private void layoutMenu() {
		OpticMenuChrome.Layout layout = optic$layout();
		@SuppressWarnings("unchecked")
		java.util.List<ButtonWidget> list = (java.util.List<ButtonWidget>) buttons;
		boolean hasMods = false;
		for (ButtonWidget button : list) if (button.id == MODS_BUTTON) hasMods = true;
		if (!hasMods) list.add(new ButtonWidget(MODS_BUTTON, 0, 0, 98, 20, "Mods"));

		// Rows: Singleplayer, Multiplayer (full width), then Options | Mods side by side.
		int half = (layout.btnW - layout.btnGap) / 2;
		int playIndex = 0;
		for (ButtonWidget button : list) {
			if (button.id == 1 || button.id == 2 || button.id == 11 || button.id == 12) {
				optic$setButtonSize(button, layout.btnW, layout.btnH);
				button.x = layout.btnX;
				button.y = layout.btnY + playIndex * (layout.btnH + layout.btnGap);
				playIndex++;
			}
		}
		int lastRow = layout.btnY + Math.max(2, playIndex) * (layout.btnH + layout.btnGap);
		for (ButtonWidget button : list) {
			if (button.id == 1 || button.id == 2 || button.id == 11 || button.id == 12) {
				continue;
			} else if (button.id == 0) {
				button.visible = true;
				optic$setButtonSize(button, half, layout.btnH);
				button.x = layout.btnX;
				button.y = lastRow;
			} else if (button.id == MODS_BUTTON) {
				button.visible = true;
				optic$setButtonSize(button, layout.btnW - half - layout.btnGap, layout.btnH);
				button.x = layout.btnX + half + layout.btnGap;
				button.y = lastRow;
			} else if (button.id == 4) {
				button.visible = true;
				optic$setButtonSize(button, layout.closeS, layout.closeS);
				button.x = layout.closeX;
				button.y = layout.closeY;
			} else if (button.id == 5 || button instanceof LanguageButton) {
				button.visible = false;
			}
		}
	}

	@Unique
	private static void optic$setButtonSize(ButtonWidget button, int w, int h) {
		ButtonWidgetAccessor access = (ButtonWidgetAccessor) button;
		access.optic$setWidth(w);
		access.optic$setHeight(h);
	}

	@Unique
	private OpticMenuChrome.Layout optic$layout() {
		return OpticMenuChrome.layout(client, width, height);
	}

	/** Skip the vanilla Minecraft title atlas draws (y=30, h=44). */
	@Redirect(
		method = "render(IIF)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawTexture(IIIIII)V"
		)
	)
	private void optic$skipVanillaLogo(TitleScreen self, int x, int y, int u, int v, int width, int height) {
		if (y == 30 && height == 44) return;
		self.drawTexture(x, y, u, v, width, height);
	}

	/** Blur the panorama and place the center card under the buttons. */
	@Inject(
		method = "render(IIF)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderBackground(IIF)V",
			shift = At.Shift.AFTER
		)
	)
	private void optic$blurBackground(int mouseX, int mouseY, float delta, CallbackInfo ci) {
		OpticMenuBlur.apply(client);
		OpticMenuChrome.drawCard(client, optic$layout());
	}

	/** Drop splash quotes and the vanilla pixel footer. */
	@Redirect(
		method = "render(IIF)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"
		)
	)
	private void optic$filterText(TitleScreen self, TextRenderer textRenderer, String text, int x, int y, int color) {
		if (text == null || text.isEmpty()) return;
		if (color == -256) return;
		if ("Minecraft 1.7.10".equals(text) || COPYRIGHT.equals(text)) return;
		self.drawWithShadow(textRenderer, text, x, y, color);
	}

	@Inject(method = "render(IIF)V", at = @At("TAIL"))
	private void optic$renderBrand(int mouseX, int mouseY, float delta, CallbackInfo ci) {
		OpticMenuChrome.Layout layout = optic$layout();
		OpticMenuChrome.drawCenterLogo(client, layout);
		OpticMenuChrome.drawTopBar(client, layout, mouseX, mouseY);
		OpticMenuChrome.drawFooter(client, width, height, layout, mouseX, mouseY);
		optic$accounts.render(client, width, mouseX, mouseY);
	}
}
