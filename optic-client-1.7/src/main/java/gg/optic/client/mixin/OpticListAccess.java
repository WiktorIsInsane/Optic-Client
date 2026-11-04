package gg.optic.client.mixin;

import net.minecraft.client.gui.widget.ListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ListWidget.class)
public interface OpticListAccess {
	@Accessor("yStart")
	int optic$getYStart();

	@Accessor("yStart")
	void optic$setYStart(int y);

	@Accessor("yEnd")
	int optic$getYEnd();
}
