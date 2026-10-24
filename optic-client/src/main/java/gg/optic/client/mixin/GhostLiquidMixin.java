package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class GhostLiquidMixin {
	@Shadow
	@Final
	private MinecraftClient client;

	@Inject(method = "breakBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z", at = @At("RETURN"))
	private void optic$fixGhostLiquid(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		OpticConfig c = OpticClient.get().config();
		if (!c.anim(c.animFixGhostLiquid) || this.client.world == null) return;
		World world = this.client.world;
		Block block = world.getBlockState(pos).getBlock();
		Material material = block.getMaterial();
		if (material == Material.WATER || material == Material.LAVA) {
			world.setAir(pos);
		}
	}
}
