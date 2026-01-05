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

  private static int getTransformedZ(int z) {
    return z + TFCRealWorldConfig.getHemisphereScale() / 2;
  }

  @Inject(method = "getLatitude", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$transformForLatitude(
    int z,
    float hemisphereScale,
    CallbackInfoReturnable<Float> cir
  ) {
    hemisphereScale = TFCRealWorldConfig.getHemisphereScale();

    int transformedZ = getTransformedZ(z);
    float triangleInput = transformedZ - 0.5f * hemisphereScale;

    float latitude = net.dries007.tfc.util.Helpers.triangle(
      -net.minecraft.util.Mth.HALF_PI,
      0,
      1 / (4 * hemisphereScale),
      triangleInput
    );

    cir.setReturnValue(latitude);
  }

  @ModifyVariable(
    method = "getInNorthernHemisphere(IF)Z",
    at = @At("HEAD"),
    argsOnly = true,
    ordinal = 0
  )
  private static int tfcrealworld$transformZForHemisphere(int z) {
    return getTransformedZ(z);
  }
}
