package net.minecraft.launchwrapper;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LaunchWrapper 1.12 with the Java 9+ classloader cast removed.
 * The classpath is read from {@code java.class.path}, which is how Optic starts the game.
 */
public class Launch {
	private static final String DEFAULT_TWEAK = "net.minecraft.launchwrapper.VanillaTweaker";
	public static File minecraftHome;
	public static File assetsDir;
	public static Map<String, Object> blackboard;
	public static LaunchClassLoader classLoader;

	public static void main(String[] args) {
		new Launch().launch(args);
	}

	private Launch() {
		classLoader = new LaunchClassLoader(classpathUrls());
		blackboard = new HashMap<String, Object>();
		Thread.currentThread().setContextClassLoader(classLoader);
	}

	private static URL[] classpathUrls() {
		String cp = System.getProperty("java.class.path", "");
		String[] parts = cp.split(File.pathSeparator);
		URL[] urls = new URL[parts.length];
		for (int i = 0; i < parts.length; i++) {
			try {
				urls[i] = new File(parts[i]).toURI().toURL();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return urls;
	}

	private void launch(String[] args) {
		OptionParser parser = new OptionParser();
		parser.allowsUnrecognizedOptions();
		OptionSpec<String> profileOption = parser.accepts("version", "The version we launched with").withRequiredArg();
		OptionSpec<File> gameDirOption = parser.accepts("gameDir", "Alternative game directory").withRequiredArg().ofType(File.class);
		OptionSpec<File> assetsDirOption = parser.accepts("assetsDir", "Assets directory").withRequiredArg().ofType(File.class);
		OptionSpec<String> tweakClassOption = parser.accepts("tweakClass", "Tweak class(es) to load").withRequiredArg().defaultsTo(DEFAULT_TWEAK);
		OptionSpec<String> nonOption = parser.nonOptions();

		OptionSet options = parser.parse(args);
		minecraftHome = options.valueOf(gameDirOption);
		assetsDir = options.valueOf(assetsDirOption);
		String profileName = options.valueOf(profileOption);
		List<String> tweakClassNames = new ArrayList<String>(options.valuesOf(tweakClassOption));
		List<String> argumentList = new ArrayList<String>();
		blackboard.put("TweakClasses", tweakClassNames);
		blackboard.put("ArgumentList", argumentList);

		Set<String> allTweakerNames = new HashSet<String>();
		List<ITweaker> allTweakers = new ArrayList<ITweaker>();
		try {
			List<ITweaker> tweakers = new ArrayList<ITweaker>(tweakClassNames.size() + 1);
			blackboard.put("Tweaks", tweakers);
			ITweaker primaryTweaker = null;
			do {
				for (Iterator<String> it = tweakClassNames.iterator(); it.hasNext(); ) {
					String tweakName = it.next();
					if (allTweakerNames.contains(tweakName)) {
						LogWrapper.log(Level.WARN, "Tweak class name %s has already been visited -- skipping", tweakName);
						it.remove();
						continue;
					}
					allTweakerNames.add(tweakName);
					LogWrapper.log(Level.INFO, "Loading tweak class name %s", tweakName);
					classLoader.addClassLoaderExclusion(tweakName.substring(0, tweakName.lastIndexOf('.')));
					ITweaker tweaker = (ITweaker) Class.forName(tweakName, true, classLoader).newInstance();
					tweakers.add(tweaker);
					it.remove();
					if (primaryTweaker == null) {
						LogWrapper.log(Level.INFO, "Using primary tweak class name %s", tweakName);
						primaryTweaker = tweaker;
					}
				}
				for (Iterator<ITweaker> it = tweakers.iterator(); it.hasNext(); ) {
					ITweaker tweaker = it.next();
					LogWrapper.log(Level.INFO, "Calling tweak class %s", tweaker.getClass().getName());
					tweaker.acceptOptions(options.valuesOf(nonOption), minecraftHome, assetsDir, profileName);
					tweaker.injectIntoClassLoader(classLoader);
					allTweakers.add(tweaker);
					it.remove();
				}
			} while (!tweakClassNames.isEmpty());

			for (ITweaker tweaker : allTweakers) {
				argumentList.addAll(Arrays.asList(tweaker.getLaunchArguments()));
			}

			String launchTarget = primaryTweaker.getLaunchTarget();
			Class<?> clazz = Class.forName(launchTarget, false, classLoader);
			Method mainMethod = clazz.getMethod("main", String[].class);
			LogWrapper.info("Launching wrapped minecraft {%s}", launchTarget);
			mainMethod.invoke(null, (Object) argumentList.toArray(new String[argumentList.size()]));
		} catch (Exception e) {
			LogWrapper.log(Level.ERROR, e, "Unable to launch");
			System.exit(1);
		}
	}
}
