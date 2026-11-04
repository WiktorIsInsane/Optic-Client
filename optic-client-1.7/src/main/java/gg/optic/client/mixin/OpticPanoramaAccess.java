package gg.optic.client.mixin;

import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TitleScreen.class)
public interface OpticPanoramaAccess {
	@Invoker("renderBackground")
	void optic$renderPanorama(int mouseX, int mouseY, float delta);

	@Accessor("backgroundTexture")
	NativeImageBackedTexture optic$getBackgroundTexture();

	@Accessor("backgroundTexture")
	void optic$setBackgroundTexture(NativeImageBackedTexture texture);

	@Accessor("backgroundTextureId")
	void optic$setBackgroundTextureId(Identifier id);

	@Accessor("ticks")
	int optic$getTicks();

	@Accessor("ticks")
	void optic$setTicks(int ticks);
}
