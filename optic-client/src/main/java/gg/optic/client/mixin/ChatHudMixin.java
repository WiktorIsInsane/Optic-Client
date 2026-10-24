package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.render.OpticChatRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
	private static final int OPTIC_SPAM_ID = -0x0C71C;

	@Shadow @Final private MinecraftClient client;

	@Shadow
	public abstract void removeMessage(int messageId);

	@Shadow
	protected abstract void addMessage(Text message, int messageId, int timestamp, boolean refresh);

	@Unique private String optic$lastRaw = "";
	@Unique private int optic$spamCount = 1;

	@Inject(method = "addMessage(Lnet/minecraft/text/Text;I)V", at = @At("HEAD"), cancellable = true)
	private void optic$chatFeatures(Text message, int messageId, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.chat || message == null) return;
		if (!c.chatAntiSpam && !c.chatBold) return;

		ci.cancel();
		Text body = message;
		int id = messageId;
		String raw = message.asUnformattedString();

		if (c.chatAntiSpam && messageId == 0) {
			if (raw.equals(optic$lastRaw)) {
				optic$spamCount++;
				this.removeMessage(OPTIC_SPAM_ID);
				body = message.copy().append(new LiteralText(" \u00a77(x" + optic$spamCount + ")"));
				id = OPTIC_SPAM_ID;
			} else {
				optic$lastRaw = raw;
				optic$spamCount = 1;
			}
		}

		if (c.chatBold) {
			body = new LiteralText("\u00a7l").append(body.copy());
		}

		this.addMessage(body, id, this.client.inGameHud.getTicks(), false);
	}

	@ModifyConstant(method = "addMessage(Lnet/minecraft/text/Text;IIZ)V", constant = @Constant(intValue = 100))
	private int optic$historyLimit(int original) {
		OpticConfig c = OpticClient.get().config();
		if (c.chat && c.chatInfiniteHistory) return 10000;
		return original;
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void optic$beginChatRender(int ticks, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		OpticChatRender.backgroundOpacity = c.chat ? c.chatBackgroundOpacity : -1.0F;
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void optic$endChatRender(int ticks, CallbackInfo ci) {
		OpticChatRender.backgroundOpacity = -1.0F;
	}
}
