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
  private static void tfcrealworld$transformForLatitude(
    int z,
    float hemisphereScale,
    CallbackInfoReturnable<Float> cir
  ) {
    int poleOffset = TFCRealWorldConfig.getPoleOffset();
    int transformedZ = z + poleOffset;

    float verticalWorldScale =
      (float) TFCRealWorldConfig.getVerticalWorldScale();
    float actualHemisphereScale = verticalWorldScale * 0.5f;

    float triangleInput = transformedZ - 0.5f * actualHemisphereScale;

    boolean poleLooping = TFCRealWorldConfig.getPoleLooping();
    if (!poleLooping && actualHemisphereScale > 0) {
      triangleInput = net.minecraft.util.Mth.clamp(
        triangleInput,
        -actualHemisphereScale,
        actualHemisphereScale
      );
    }

    float latitude = net.dries007.tfc.util.Helpers.triangle(
      -net.minecraft.util.Mth.HALF_PI,
      0,
      1 / (4 * actualHemisphereScale),
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
    int poleOffset = TFCRealWorldConfig.getPoleOffset();
    return z + poleOffset;
  }

  @Inject(
    method = "getInNorthernHemisphere(IF)Z",
    at = @At("HEAD"),
    cancellable = true
  )
  private static void tfcrealworld$overrideHemisphereCheck(
    int z,
    float hemisphereScale,
    CallbackInfoReturnable<Boolean> cir
  ) {
    boolean poleLooping = TFCRealWorldConfig.getPoleLooping();
    if (!poleLooping) {
      float verticalWorldScale =
        (float) TFCRealWorldConfig.getVerticalWorldScale();
      float actualHemisphereScale = verticalWorldScale * 0.5f;
      int adjustedZ = z - (int) (actualHemisphereScale / 2);
      int poleToPoleDistance = (int) (actualHemisphereScale * 2);

      if (adjustedZ < -poleToPoleDistance) {
        cir.setReturnValue(false);
      } else if (adjustedZ > poleToPoleDistance) {
        cir.setReturnValue(true);
      } else {
        cir.setReturnValue(adjustedZ > 0);
      }
    }
  }
}
