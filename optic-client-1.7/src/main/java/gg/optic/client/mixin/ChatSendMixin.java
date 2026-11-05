package gg.optic.client.mixin;

import gg.optic.client.OpticScreenshots;
import net.minecraft.entity.player.ControllablePlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optic's own chat commands (screenshot [Copy]) run locally and are never sent to the server. */
@Mixin(ControllablePlayerEntity.class)
public abstract class ChatSendMixin {
	@Inject(method = "method_1262(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void optic$localCommands(String message, CallbackInfo ci) {
		if (OpticScreenshots.handleCommand(message)) ci.cancel();
	}
}
