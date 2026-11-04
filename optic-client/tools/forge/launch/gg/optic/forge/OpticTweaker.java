package gg.optic.forge;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads Optic's mixins. With OptiFine on the classpath OptiFine stays the primary
 * tweaker and passes the game arguments on. Without it (plain 1.7.10) this is the
 * primary tweaker, so it hands launchwrapper's consumed arguments back to Main.
 */
public class OpticTweaker implements ITweaker {
	private final List<String> args = new ArrayList<String>();

	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
		this.args.clear();
		if (hasOptiFine()) return;
		this.args.addAll(args);
		if (profile != null) {
			this.args.add("--version");
			this.args.add(profile);
		}
		if (gameDir != null) {
			this.args.add("--gameDir");
			this.args.add(gameDir.getAbsolutePath());
		}
		if (assetsDir != null) {
			this.args.add("--assetsDir");
			this.args.add(assetsDir.getAbsolutePath());
		}
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
		return args.toArray(new String[0]);
	}

	private static boolean hasOptiFine() {
		return OpticTweaker.class.getClassLoader().getResource("optifine/OptiFineTweaker.class") != null;
	}
}
