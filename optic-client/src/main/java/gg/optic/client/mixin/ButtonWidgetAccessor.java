package gg.optic.client.mixin;

import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ButtonWidget.class)
public interface ButtonWidgetAccessor {
	@Accessor("width")
	void optic$setWidth(int width);

	@Accessor("height")
	void optic$setHeight(int height);
}
