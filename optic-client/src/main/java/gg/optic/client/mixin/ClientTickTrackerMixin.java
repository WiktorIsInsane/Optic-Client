package gg.optic.client.mixin;

import net.minecraft.client.render.ClientTickTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla's tick clock mixes two timers and then speeds itself up to catch the drift.
 * That bunches movement and clicks, and the catch-up can run the game faster than 20
 * ticks a second. This keeps one real-time tick every 50ms and never banks extra speed.
 */
@Mixin(ClientTickTracker.class)
public class ClientTickTrackerMixin {
	private static final double MAX_CATCH_UP_TICKS = 20.0;
	private static final int MAX_TICKS_THIS_FRAME = 10;

	@Shadow
	public int ticksThisFrame;

	@Shadow
	public float tickDelta;

	@Shadow
	public float speed;

	@Shadow
	public float lastFrameDuration;

	@Shadow
	float ticksPerSecond;

	@Unique
	private long optic$lastNanos;

	@Unique
	private boolean optic$started;

	@Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
	private void optic$realTimeTicks(CallbackInfo ci) {
		long now = System.nanoTime();
		if (!this.optic$started) {
			this.optic$started = true;
			this.optic$lastNanos = now;
			this.ticksThisFrame = 0;
			this.tickDelta = 0.0F;
			ci.cancel();
			return;
		}

		long elapsed = now - this.optic$lastNanos;
		this.optic$lastNanos = now;
		if (elapsed < 0L) {
			this.ticksThisFrame = 0;
			this.tickDelta = this.lastFrameDuration;
			ci.cancel();
			return;
		}

		double tickNanos = 1_000_000_000.0 / this.ticksPerSecond;
		double add = elapsed / tickNanos;
		if (add > MAX_CATCH_UP_TICKS) {
			add = MAX_CATCH_UP_TICKS;
		}
		float rate = this.speed;
		if (rate < 0.0F) {
			rate = 0.0F;
		} else if (rate > 1.0F) {
			rate = 1.0F;
		}
		this.lastFrameDuration += (float) (add * rate);

		int ticks = (int) this.lastFrameDuration;
		float fraction = this.lastFrameDuration - ticks;
		if (ticks > MAX_TICKS_THIS_FRAME) {
			ticks = MAX_TICKS_THIS_FRAME;
		}
		this.lastFrameDuration = fraction;
		this.ticksThisFrame = ticks;
		this.tickDelta = fraction;
		ci.cancel();
	}
}
