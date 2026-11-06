package gg.optic.client.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

/**
 * PotPvP pearl cooldown, Badlion style: the pearl icon with the seconds left and a bar that
 * empties. Starts when you throw an ender pearl (1.7 has no cooldown of its own, so the length
 * is a setting to match the server, 15 s on most PotPvP servers).
 */
public final class OpticPearl {
	static final int W = 58;
	static final int H = 20;
	private static final Identifier PEARL_ICON = new Identifier("textures/items/ender_pearl.png");
	private static long startedAt = -1L;

	private OpticPearl() {
	}

	/** Right-click: throwing a pearl starts the timer (not again while it is still running). */
	public static void onUse(MinecraftClient client, OpticConfig c) {
		if (!c.pvpTweaks || !c.pvpPearlCooldown || client.field_3805 == null || client.currentScreen != null) return;
		ItemStack held = client.field_3805.getMainHandStack();
		if (held == null || held.getItem() != Items.ENDER_PEARL) return;
		if (remaining(c) > 0.0F) return;
		startedAt = System.currentTimeMillis();
	}

	/** Seconds left, 0 when ready. */
	public static float remaining(OpticConfig c) {
		if (startedAt < 0L) return 0.0F;
		float left = c.pvpPearlSeconds - (System.currentTimeMillis() - startedAt) / 1000.0F;
		return Math.max(0.0F, left);
	}

	static void render(MinecraftClient client, OpticConfig c, boolean preview) {
		float left = remaining(c);
		if (left <= 0.0F && !preview) return;
		float shown = left > 0.0F ? left : c.pvpPearlSeconds;
		float s = c.pearlScale;
		GlStateManager.pushMatrix();
		GlStateManager.translate(c.pearlX, c.pearlY, 0.0F);
		GlStateManager.scale(s, s, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		DrawableHelper.fill(0, 0, W, H, 0x80000000);

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		client.getTextureManager().bindTexture(PEARL_ICON);
		DrawableHelper.drawTexture(2, 2, 0.0F, 0.0F, 16, 16, 16.0F, 16.0F);

		String text = String.format("%.1fs", shown);
		int color = left > 0.0F ? 0xFFFFFFFF : 0xFF5CFF8A;
		client.textRenderer.draw(text, 21, 4, color, true);

		// Bar along the bottom: full when thrown, empty when the pearl is ready again.
		float frac = c.pvpPearlSeconds <= 0.0F ? 0.0F : shown / c.pvpPearlSeconds;
		int barW = Math.round((W - 23) * Math.max(0.0F, Math.min(1.0F, frac)));
		DrawableHelper.fill(21, H - 4, W - 2, H - 2, 0x60FFFFFF);
		if (barW > 0) DrawableHelper.fill(21, H - 4, 21 + barW, H - 2, 0xFFB46CFF);
		GlStateManager.popMatrix();
	}
}
