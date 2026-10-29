package gg.optic.client.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.HeldItemRenderer;
import net.minecraft.client.render.entity.model.BiPedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemFeatureMixin {
	@Shadow
	@Final
	private LivingEntityRenderer<?> entityRenderer;

	@Inject(
		method = "render(Lnet/minecraft/entity/LivingEntity;FFFFFFF)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void optic$oldThirdPerson(
		LivingEntity entity,
		float handSwing,
		float handSwingAmount,
		float tickDelta,
		float age,
		float headYaw,
		float headPitch,
		float scale,
		CallbackInfo ci
	) {
		OpticConfig c = OpticClient.get().config();
		boolean oldHold = c.anim(c.animItemHold);
		boolean oldBlock = c.anim(c.animBlockHit);
		if (!oldHold && !oldBlock) return;

		ItemStack stack = entity.getStackInHand();
		if (stack == null) return;
		ci.cancel();

		GlStateManager.pushMatrix();
		if (this.entityRenderer.getModel().child) {
			float f = 0.5F;
			GlStateManager.translate(0.0F, 0.625F, 0.0F);
			GlStateManager.rotate(-20.0F, -1.0F, 0.0F, 0.0F);
			GlStateManager.scale(f, f, f);
		}

		boolean blocking = entity instanceof PlayerEntity && ((PlayerEntity) entity).method_2611();
		BiPedModel model = (BiPedModel) this.entityRenderer.getModel();

		if (blocking && oldBlock) {
			model.rightArm.posX = model.rightArm.posX * 0.5F - ((float) Math.PI / 10.0F);
			model.setArmAngle(0.0625F);
			GlStateManager.translate(-0.0625F, 0.4375F, 0.0625F);
		} else if (oldHold && !(blocking && oldBlock)) {
			model.setArmAngle(0.0625F);
			GlStateManager.translate(-0.0855F, 0.4775F, 0.1585F);
			GlStateManager.rotate(-19.0F, 20.0F, 0.0F, -6.0F);
		} else {
			model.setArmAngle(0.0625F);
			GlStateManager.translate(-0.0625F, 0.4375F, 0.0625F);
		}

		if (entity instanceof PlayerEntity && ((PlayerEntity) entity).fishHook != null) {
			stack = new ItemStack(Items.FISHING_ROD, 0);
		}

		Item item = stack.getItem();
		MinecraftClient client = MinecraftClient.getInstance();
		if (item instanceof BlockItem && Block.getBlockFromItem(item).getBlockType() == 2) {
			GlStateManager.translate(0.0F, 0.1875F, -0.3125F);
			GlStateManager.rotate(20.0F, 1.0F, 0.0F, 0.0F);
			GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
			float g = 0.375F;
			GlStateManager.scale(-g, -g, g);
		}

		if (entity.isSneaking()) {
			GlStateManager.translate(0.0F, 0.203125F, 0.0F);
		}

		client.getHeldItemRenderer().renderItem(entity, stack, ModelTransformation.Mode.THIRD_PERSON);
		GlStateManager.popMatrix();
	}
}
