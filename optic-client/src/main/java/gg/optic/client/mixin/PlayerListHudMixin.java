package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.hud.OpticTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
	@Shadow @Final private MinecraftClient client;
	@Shadow private Text header;
	@Shadow private Text footer;

	@Inject(
		method = "render(ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void optic$tab(int width, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
		if (!OpticClient.get().config().tab) return;
		ci.cancel();
		if (!OpticTab.visible(client)) return;
		OpticTab.render(client, width, scoreboard, objective, header, footer);
	}
}
