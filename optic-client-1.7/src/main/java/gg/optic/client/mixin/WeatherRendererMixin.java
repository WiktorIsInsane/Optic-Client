package gg.optic.client.mixin;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticEnvironment;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.world.LayeredBiomeSource;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Weather Changer: the forced weather looks the same in every biome. Vanilla skips biomes that
 * never rain (deserts) and picks rain or snow from biome temperature, so "Snow" only ever showed
 * rain in warm biomes. Client visuals only.
 */
@Mixin(GameRenderer.class)
public abstract class WeatherRendererMixin {
	private static int optic$mode() {
		OpticConfig c = OpticClient.get().config();
		return c.weatherChanger ? c.weatherMode : -1;
	}

	private static boolean optic$forcedPrecipitation() {
		int mode = optic$mode();
		return mode == OpticEnvironment.WEATHER_RAIN || mode == OpticEnvironment.WEATHER_SNOW || mode == OpticEnvironment.WEATHER_STORM;
	}

	/** Rain/snow columns: draw them in every biome while weather is forced. */
	@Redirect(
		method = "renderWeather(F)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;method_3830()Z"),
		require = 0
	)
	private boolean optic$weatherEverywhere(Biome biome) {
		return optic$forcedPrecipitation() || biome.method_3830();
	}

	/**
	 * Rain/Snow Opacity: rain and snow fade with the world's rain strength, so scaling it only
	 * here (not for the sky or sound) makes the drops see-through. 0% hides them.
	 */
	@Redirect(
		method = "renderWeather(F)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getRainGradient(F)F"),
		require = 0
	)
	private float optic$weatherOpacity(net.minecraft.client.world.ClientWorld world, float tickDelta) {
		float strength = world.getRainGradient(tickDelta);
		OpticConfig c = OpticClient.get().config();
		if (!c.weatherChanger) return strength;
		return strength * Math.max(0.0F, Math.min(100.0F, c.weatherOpacity)) / 100.0F;
	}

	/** Rain Color: tints raindrops (the first weather colour call). */
	@Redirect(
		method = "renderWeather(F)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;color(FFFF)V", ordinal = 0),
		require = 0
	)
	private void optic$rainColor(net.minecraft.client.render.Tessellator tessellator, float r, float g, float b, float a) {
		OpticConfig c = OpticClient.get().config();
		if (c.weatherChanger) {
			int rain = c.weatherRainColor;
			r *= ((rain >> 16) & 0xFF) / 255.0F;
			g *= ((rain >> 8) & 0xFF) / 255.0F;
			b *= (rain & 0xFF) / 255.0F;
			a *= OpticConfig.opacityOf(rain) / 255.0F;
		}
		tessellator.color(r, g, b, a);
	}

	/** Snow Color: tints snowflakes (the second weather colour call; the first is rain). */
	@Redirect(
		method = "renderWeather(F)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;color(FFFF)V", ordinal = 1),
		require = 0
	)
	private void optic$snowColor(net.minecraft.client.render.Tessellator tessellator, float r, float g, float b, float a) {
		OpticConfig c = OpticClient.get().config();
		if (c.weatherChanger) {
			int snow = c.weatherSnowColor;
			r *= ((snow >> 16) & 0xFF) / 255.0F;
			g *= ((snow >> 8) & 0xFF) / 255.0F;
			b *= (snow & 0xFF) / 255.0F;
			a *= OpticConfig.opacityOf(snow) / 255.0F;
		}
		tessellator.color(r, g, b, a);
	}

	/** Below 0.15 vanilla draws snow, otherwise rain. */
	@Redirect(
		method = "renderWeather(F)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/LayeredBiomeSource;method_3852(FI)F"),
		require = 0
	)
	private float optic$snowOrRain(LayeredBiomeSource source, float temperature, int y) {
		int mode = optic$mode();
		if (mode == OpticEnvironment.WEATHER_SNOW) return 0.0F;
		if (mode == OpticEnvironment.WEATHER_RAIN || mode == OpticEnvironment.WEATHER_STORM) return 1.0F;
		return source.method_3852(temperature, y);
	}

	/** Rain splashes and rain sound: everywhere for rain, nowhere for snow. */
	@Redirect(
		method = "tickRainSplashing()V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;method_3830()Z"),
		require = 0
	)
	private boolean optic$splashEverywhere(Biome biome) {
		int mode = optic$mode();
		if (mode == OpticEnvironment.WEATHER_SNOW) return false;
		if (mode == OpticEnvironment.WEATHER_RAIN || mode == OpticEnvironment.WEATHER_STORM) return true;
		return biome.method_3830();
	}

	@Redirect(
		method = "tickRainSplashing()V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;method_6418(III)F"),
		require = 0
	)
	private float optic$splashTemperature(Biome biome, int x, int y, int z) {
		int mode = optic$mode();
		if (mode == OpticEnvironment.WEATHER_RAIN || mode == OpticEnvironment.WEATHER_STORM) return 1.0F;
		return biome.method_6418(x, y, z);
	}
}
