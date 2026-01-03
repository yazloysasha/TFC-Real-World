package net.yazloysasha.tfcrealworld.mixin.client.overworld;

import net.dries007.tfc.client.overworld.SolarCalculator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SolarCalculator.class)
public class SolarCalculatorMixin {

  @Inject(method = "getLatitude", at = @At("HEAD"), cancellable = true)
  private static void realworld$transformForLatitude(
    int z,
    float hemisphereScale,
    CallbackInfoReturnable<Float> cir
  ) {
    int poleOffset = TFCRealWorldConfig.POLE_OFFSET.get();
    int transformedZ = z + poleOffset;

    float latitude = net.dries007.tfc.util.Helpers.triangle(
      -net.minecraft.util.Mth.HALF_PI,
      0,
      1 / (4 * hemisphereScale),
      transformedZ - 0.5f * hemisphereScale
    );

    boolean poleLooping = TFCRealWorldConfig.POLE_LOOPING.get();
    if (!poleLooping && hemisphereScale > 0) {
      latitude = net.minecraft.util.Mth.clamp(
        latitude,
        -net.minecraft.util.Mth.HALF_PI,
        net.minecraft.util.Mth.HALF_PI
      );
    }

    cir.setReturnValue(latitude);
  }

  @ModifyVariable(
    method = "getInNorthernHemisphere(IF)Z",
    at = @At("HEAD"),
    argsOnly = true,
    ordinal = 0
  )
  private static int realworld$transformZForHemisphere(int z) {
    int poleOffset = TFCRealWorldConfig.POLE_OFFSET.get();
    return z + poleOffset;
  }
}
