package gg.optic.client.mixin;

import gg.optic.client.render.OpticEntityCull;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
	@Inject(method = "renderEntity(Lnet/minecraft/block/entity/BlockEntity;FI)V", at = @At("HEAD"), cancellable = true)
	private void optic$cullBlockEntity(BlockEntity blockEntity, float tickDelta, int breakStage, CallbackInfo ci) {
		if (OpticEntityCull.hiddenBlock(blockEntity)) ci.cancel();
	}
}
