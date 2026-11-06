package gg.optic.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Writes MinecraftClient.attackCooldown (the left-click lockout after a miss). */
@Mixin(MinecraftClient.class)
public interface OpticAttackCooldownAccess {
	@Accessor("attackCooldown")
	void optic$setAttackCooldown(int value);
}
