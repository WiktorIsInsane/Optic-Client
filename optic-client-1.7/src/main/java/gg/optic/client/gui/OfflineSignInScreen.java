package gg.optic.client.gui;

import gg.optic.client.OpticUiDraw;
import gg.optic.client.OpticUiFont;
import gg.optic.client.account.OfflineAccounts;
import gg.optic.client.account.SessionSwap;
import gg.optic.client.account.SharedAccount;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.input.Keyboard;

public final class OfflineSignInScreen extends Screen {
	private final Screen parent;
	private TextFieldWidget field;
	private String error = "";

	public OfflineSignInScreen(Screen parent) {
		this.parent = parent;
	}

	@Override
	public void init() {
		Keyboard.enableRepeatEvents(true);
		field = new TextFieldWidget(textRenderer, width / 2 - 100, height / 2 - 10, 200, 20);
		field.setMaxLength(16);
		field.setFocused(true);
		buttons.clear();
		buttons.add(new ButtonWidget(1, width / 2 - 100, height / 2 + 24, 98, 20, "Sign in"));
		buttons.add(new ButtonWidget(2, width / 2 + 2, height / 2 + 24, 98, 20, "Cancel"));
	}

	@Override
	public void removed() {
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	public void tick() {
		field.tick();
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == 2) {
			client.setScreen(parent);
			return;
		}
		if (button.id == 1) submit();
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			client.setScreen(parent);
			return;
		}
		if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
			submit();
			return;
		}
		field.keyPressed(character, keyCode);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);
		field.mouseClicked(mouseX, mouseY, button);
	}

	private void submit() {
		try {
			SharedAccount account = OfflineAccounts.create(field.getText());
			SessionSwap.applyNew(client, account);
			client.setScreen(parent);
		} catch (IllegalArgumentException e) {
			error = e.getMessage();
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		renderBackground();
		int panelW = 240;
		int panelH = 120;
		int panelX = width / 2 - panelW / 2;
		int panelY = height / 2 - 52;
		OpticUiDraw.fillExact(client, panelX, panelY, panelW, panelH, 10, 0xF0101014);
		OpticUiDraw.outlineExact(client, panelX, panelY, panelW, panelH, 10, 0x555CFF8A);
		String title = "Offline account";
		OpticUiFont.drawSharp(client, title, width / 2 - OpticUiFont.widthSharp(client, title, 0xFF5CFF8A, 11.0F) / 2, panelY + 10, 0xFF5CFF8A, 11.0F);
		String hint = "3-16 letters, numbers, underscore";
		OpticUiFont.drawSharp(client, hint, width / 2 - OpticUiFont.widthSharp(client, hint, 0xFFAAAAAA, 8.0F) / 2, panelY + 26, 0xFFAAAAAA, 8.0F);
		field.render();
		if (!error.isEmpty()) {
			OpticUiFont.drawSharp(client, error, width / 2 - OpticUiFont.widthSharp(client, error, 0xFFFF5555, 8.0F) / 2, height / 2 + 52, 0xFFFF5555, 8.0F);
		}
		super.render(mouseX, mouseY, delta);
	}
}
