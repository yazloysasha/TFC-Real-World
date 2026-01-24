package net.yazloysasha.tfcrealworld.mixin.client.overworld;

import net.dries007.tfc.client.overworld.SolarCalculator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SolarCalculator.class)
public class SolarCalculatorMixin {

  private static int transformCoordForLatitude(int value, int scale) {
    int tileRadius = scale;
    int tileDiameter = scale * 2;
    int tileValue = (int) Math.floor(
      (value + tileRadius) / (double) tileDiameter
    );
    int localValue = value - tileValue * tileDiameter;

    if (Math.floorMod(tileValue, 2) != 0) {
      localValue = -localValue;
    }

    return Math.clamp(localValue, -tileRadius, tileRadius);
  }

  private static double getLatitudeByZ(int z) {
    return ProjectionManager.getLatitudeByZ(
      transformCoordForLatitude(z, TFCRealWorldConfig.VERTICAL_SCALE.get())
    );
  }

  @Inject(method = "getLatitude", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$transformForLatitude(
    int z,
    float hemisphereScale,
    CallbackInfoReturnable<Float> cir
  ) {
    double latitudeDegrees = getLatitudeByZ(z);
    float latitude = (float) Math.toRadians(latitudeDegrees);

    cir.setReturnValue(latitude);
  }

  @Inject(
    method = "getInNorthernHemisphere(IF)Z",
    at = @At("HEAD"),
    cancellable = true
  )
  private static void tfcrealworld$transformForHemisphere(
    int z,
    float hemisphereScale,
    CallbackInfoReturnable<Boolean> cir
  ) {
    double latitude = getLatitudeByZ(z);

    cir.setReturnValue(latitude > 0);
  }
}
