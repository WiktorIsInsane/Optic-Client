package gg.optic.client.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
	@Shadow
	@Final
	private MinecraftClient client;

	@Shadow
	private ItemStack mainHand;

	@Unique
	private float optic$swingProgress;

	@Unique
	private boolean optic$itemPosed;

	@Unique
	private boolean optic$leftHand;

	@Inject(method = "renderArmHoldingItem(F)V", at = @At("HEAD"))
	private void optic$captureSwing(float tickDelta, CallbackInfo ci) {
		this.optic$itemPosed = false;
		if (this.client.player != null) {
			this.optic$swingProgress = this.client.player.getHandSwingProgress(tickDelta);
		}
		OpticConfig c = OpticClient.get().config();
		this.optic$leftHand = c.anim(c.animLeftHand);
		if (this.optic$leftHand) {
			GlStateManager.pushMatrix();
			GlStateManager.scale(-1.0F, 1.0F, 1.0F);
			GlStateManager.cullFace(1028);
		}
	}

	@Inject(method = "renderArmHoldingItem(F)V", at = @At("RETURN"))
	private void optic$restoreCull(float tickDelta, CallbackInfo ci) {
		if (!this.optic$leftHand) return;
		GlStateManager.cullFace(1029);
		GlStateManager.popMatrix();
	}

	@Inject(method = "updateHeldItems()V", at = @At("HEAD"))
	private void optic$blockHitSwing(CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.anim(c.animBlockHit) || this.client.player == null) return;
		if (!this.client.options.attackKey.isPressed() || !this.client.options.useKey.isPressed()) return;
		ClientPlayerEntity player = this.client.player;
		if (!player.handSwinging || player.handSwingTicks >= 3 || player.handSwingTicks < 0) {
			player.handSwingTicks = -1;
			player.handSwinging = true;
		}
	}

	@ModifyConstant(
		method = "renderArmHoldingItem(F)V",
		slice = @Slice(
			from = @At(
				value = "INVOKE",
				target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/util/UseAction;"
			),
			to = @At(
				value = "INVOKE",
				target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applySwordBlockTransformation()V"
			)
		),
		constant = @Constant(floatValue = 0.0F)
	)
	private float optic$swingWhileUsing(float original) {
		OpticConfig c = OpticClient.get().config();
		if (!c.anim(c.animBlockHit) && !c.anim(c.animSwordSwing)) return original;
		return this.optic$swingProgress;
	}

	@Inject(
		method = "renderArmHoldingItem(F)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applySwordBlockTransformation()V",
			shift = At.Shift.AFTER
		)
	)
	private void optic$oldBlockTransform(float tickDelta, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.anim(c.animItemHold) || this.mainHand == null) return;
		if (this.client.player != null && this.client.player.getItemUseTicks() > 0) return;
		GlStateManager.scale(0.83F, 0.88F, 0.85F);
		GlStateManager.translate(-0.3F, 0.1F, 0.0F);
	}

	/** 1.7 sword block: across the body, and the swing from the same hand still plays. */
	@Inject(method = "applySwordBlockTransformation()V", at = @At("HEAD"), cancellable = true)
	private void optic$sevenBlock(CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.anim(c.animBlockHit)) return;
		ci.cancel();
		GlStateManager.translate(-0.5F, 0.2F, 0.0F);
		GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
		GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
	}

	@Inject(
		method = "renderArmHoldingItem(F)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"
		)
	)
	private void optic$oldRodAndThrow(float tickDelta, CallbackInfo ci) {
		if (this.optic$itemPosed || this.mainHand == null) return;
		this.optic$itemPosed = true;
		OpticConfig c = OpticClient.get().config();
		Item item = this.mainHand.getItem();
		if (c.anim(c.animFishingRod) && item == Items.FISHING_ROD) {
			GlStateManager.translate(0.08F, -0.027F, -0.33F);
			GlStateManager.scale(0.93F, 1.0F, 1.0F);
		}
		if (c.anim(c.animThrowing) && isThrowable(item)) {
			GlStateManager.translate(0.0F, 0.05F, -0.2F);
		}
	}

	/**
	 * 1.7 holds tools a little higher and turned out. This has to run after the
	 * hand is already placed. Doing it earlier rotates the item around the camera
	 * and drops it down the screen.
	 */
	@Inject(
		method = "renderArmHoldingItem(F)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;)V"
		)
	)
	private void optic$oldItemHold(float tickDelta, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.anim(c.animItemHold) || this.mainHand == null) return;
		if (this.client.player != null && this.client.player.getItemUseTicks() > 0) return;
		Item item = this.mainHand.getItem();
		if (item instanceof BlockItem || item == Items.FISHING_ROD || isThrowable(item)) return;
		float scale = 0.7585F / 0.86F;
		GlStateManager.scale(scale, scale, scale);
		GlStateManager.translate(-0.084F, 0.059F, 0.08F);
		GlStateManager.rotate(5.0F, 0.0F, 1.0F, 0.0F);
	}

	@Inject(
		method = "renderArmHoldingItem(F)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyBowTransformation(FLnet/minecraft/client/network/AbstractClientPlayerEntity;)V",
			shift = At.Shift.AFTER
		)
	)
	private void optic$oldBow(float tickDelta, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.anim(c.animBow)) return;
		GlStateManager.translate(0.0F, 0.1F, -0.15F);
	}

	@Inject(method = "applyEatOrDrinkTransformation(Lnet/minecraft/client/network/AbstractClientPlayerEntity;F)V", at = @At("HEAD"), cancellable = true)
	private void optic$oldConsume(AbstractClientPlayerEntity player, float tickDelta, CallbackInfo ci) {
		OpticConfig c = OpticClient.get().config();
		if (!c.anim(c.animEating) || this.mainHand == null) return;
		ci.cancel();
		float useAmount = (float) player.getItemUseTicks() - tickDelta + 1.0F;
		float f1 = 1.0F - useAmount / (float) this.mainHand.getMaxUseTime();
		float f2 = 1.0F - f1;
		f2 = f2 * f2 * f2;
		f2 = f2 * f2 * f2;
		f2 = f2 * f2 * f2;
		float f3 = 1.0F - f2;
		GlStateManager.translate(0.0F, MathHelper.abs(MathHelper.cos(useAmount / 4.0F * (float) Math.PI) * 0.1F) * (f1 > 0.2F ? 1.0F : 0.0F), 0.0F);
		GlStateManager.translate(f3 * 0.6F, -f3 * 0.5F, 0.0F);
		GlStateManager.rotate(f3 * 90.0F, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(f3 * 10.0F, 1.0F, 0.0F, 0.0F);
		GlStateManager.rotate(f3 * 30.0F, 0.0F, 0.0F, 1.0F);
	}

	@ModifyConstant(method = "updateHeldItems()V", constant = @Constant(floatValue = 0.4F))
	private float optic$fastItemSwitch(float original) {
		OpticConfig c = OpticClient.get().config();
		return c.anim(c.animItemSwitch) ? 1.0F : original;
	}

	@Unique
	private static boolean isThrowable(Item item) {
		return item == Items.SNOWBALL
			|| item == Items.EGG
			|| item == Items.ENDER_PEARL
			|| item == Items.EXPERIENCE_BOTTLE
			|| item == Items.POTION
			|| item == Items.FIRE_CHARGE;
	}
}
