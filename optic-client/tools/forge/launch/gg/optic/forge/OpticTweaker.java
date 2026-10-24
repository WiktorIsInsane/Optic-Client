package gg.optic.forge;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.util.List;

/** Loads Optic's mixins beside OptiFine. OptiFine stays the primary tweaker. */
public class OpticTweaker implements ITweaker {
	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader) {
		MixinBootstrap.init();
		MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
		Mixins.addConfiguration("optic.mixins.json");
	}

	@Override
	public String getLaunchTarget() {
		return "net.minecraft.client.main.Main";
	}

	@Override
	public String[] getLaunchArguments() {
		return new String[0];
	}
}
