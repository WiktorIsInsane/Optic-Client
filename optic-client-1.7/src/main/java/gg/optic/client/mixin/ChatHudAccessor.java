package gg.optic.client.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Chat lines for Copy Chat Messages: full messages, drawn (wrapped) lines, scroll. */
@Mixin(ChatHud.class)
public interface ChatHudAccessor {
	@Accessor("messages")
	List<?> optic$messages();

	@Accessor("visibleMessages")
	List<?> optic$visibleMessages();

	@Accessor("scrolledLines")
	int optic$scrolledLines();
}
