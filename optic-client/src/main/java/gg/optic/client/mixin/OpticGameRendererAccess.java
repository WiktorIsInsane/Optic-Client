package gg.optic.client.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface OpticGameRendererAccess {
	@Accessor("shadersEnabled")
	boolean optic$shadersEnabled();

	@Accessor("thirdPersonDistance")
	float optic$getThirdPersonDistance();

	@Accessor("thirdPersonDistance")
	void optic$setThirdPersonDistance(float distance);

	@Accessor("lastThirdPersonDistance")
	void optic$setLastThirdPersonDistance(float distance);
}
