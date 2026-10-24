package gg.optic.client.mixin;

import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerEntity.class)
public interface OpticPlayerAccess {
	/** Vanilla look/position packet. Not called from the click path: a second one in the same tick is a timer flag. */
	@Invoker("sendMovementPackets")
	void optic$sendMovementPackets();
}
