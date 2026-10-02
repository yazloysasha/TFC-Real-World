package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.AnnotateClimate;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainVarianceNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Climate maps already hold the final climate (coasts, currents and rainfall
 * seasonality included), so with them every point takes the map values as is,
 * without vanilla's ocean and cell-edge biases. Procedural climate only gets
 * the temperature band shifted by the configured scale.
 */
@Mixin(value = AnnotateClimate.class, remap = false)
public class AnnotateClimateMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$climateFromMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!TFCRealWorldConfig.CLIMATE_FROM_MAP.get()) {
      return;
    }
    final RegionGenerator generator = context.generator();
    for (final Region.Point point : context.region.points()) {
      final int x = point.x;
      final int z = point.z;
      point.temperature = (float) generator.temperatureNoise.noise(x, z);
      point.rainfall = Math.clamp(
        (float) generator.rainfallNoise.noise(x, z),
        0,
        500
      );
      point.rainfallVariance = PNGRainVarianceNoise.calendarVariance(
        generator.rainfallVarianceNoise.noise(x, z),
        z
      );
    }
    ci.cancel();
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/noise/Noise2D;noise(DD)D",
      ordinal = 0
    )
  )
  private double tfcrealworld$shiftTemperatureBand(
    Noise2D instance,
    double x,
    double z
  ) {
    final int temperatureScale = TFCRealWorldConfig.TEMPERATURE_SCALE.get();
    if (temperatureScale > 0) {
      final double offsetInGrid =
        (double) (-temperatureScale / 2) / Units.GRID_WIDTH_IN_BLOCK;
      return instance.noise(x, z - offsetInGrid);
    }
    return instance.noise(x, z);
  }
}
