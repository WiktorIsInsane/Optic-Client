package gg.optic.client.mixin;

import gg.optic.client.OpticZoom;
import gg.optic.client.input.OpticRawInput;
import net.minecraft.client.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseInput.class)
public class MouseInputMixin {
	@Shadow public int x;
	@Shadow public int y;

	@Inject(method = "updateMouse()V", at = @At("HEAD"), cancellable = true)
	private void optic$rawMouse(CallbackInfo ci) {
		if (!OpticRawInput.isActive()) return;
		this.x = OpticRawInput.consumeDx();
		this.y = OpticRawInput.consumeDy();
		optic$applyZoomSens();
		ci.cancel();
	}

	@Inject(method = "updateMouse()V", at = @At("RETURN"))
	private void optic$zoomSens(CallbackInfo ci) {
		if (OpticRawInput.isActive()) return;
		optic$applyZoomSens();
	}

	private void optic$applyZoomSens() {
		this.x = OpticZoom.scaleMouseDelta(this.x, true);
		this.y = OpticZoom.scaleMouseDelta(this.y, false);
	}
}
