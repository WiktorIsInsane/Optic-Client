package gg.optic.client.render;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.entity.LivingEntity;

/** Custom hurt tint. Armor uses it only while that option is on. */
public final class OpticHitColor {
	public static LivingEntity armorEntity;

	private OpticHitColor() {
	}

	public static boolean enabled() {
		return OpticClient.get().config().hitColor;
	}

	public static boolean tintArmor() {
		OpticConfig c = OpticClient.get().config();
		if (!c.hitColor || !c.hitColorArmor || armorEntity == null) return false;
		return armorEntity.hurtTime > 0 || armorEntity.deathTime > 0;
	}

	public static float red() {
		return ((OpticClient.get().config().hitColorRgb >> 16) & 255) / 255.0F;
	}

	public static float green() {
		return ((OpticClient.get().config().hitColorRgb >> 8) & 255) / 255.0F;
	}

	public static float blue() {
		return (OpticClient.get().config().hitColorRgb & 255) / 255.0F;
	}

	public static float alpha() {
		return Math.max(0.05F, Math.min(1.0F, OpticClient.get().config().hitColorAlpha));
	}
}
