package net.yazloysasha.tfcrealworld.mixin.world.region;

import java.util.Iterator;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.AnnotateClimate;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AnnotateClimate.class)
public class AnnotateClimateMixin {

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/noise/Noise2D;noise(DD)D",
      ordinal = 0
    )
  )
  private double tfcrealworld$transformZForTemperature(
    Noise2D instance,
    double x,
    double z
  ) {
    if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return instance.noise(x, z);
    }

    int temperatureScale = TFCRealWorldConfig.TEMPERATURE_SCALE.get();
    if (temperatureScale > 0) {
      double offsetInGrid =
        (double) (-temperatureScale / 2) / Units.GRID_WIDTH_IN_BLOCK;
      return instance.noise(x, z - offsetInGrid);
    }

    return instance.noise(x, z);
  }

  /**
   * Overrides rainfallVariance calculation when using map.
   */
  @Inject(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/util/Mth;lerp(FFF)F",
      ordinal = 0
    ),
    locals = LocalCapture.CAPTURE_FAILHARD,
    cancellable = false
  )
  private void tfcrealworld$overrideRainfallVariance(
    RegionGenerator.Context context,
    CallbackInfo ci,
    Iterator<?> iterator,
    Region.Point point,
    int x,
    int z,
    float bias
  ) {
    if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      point.rainfallVariance = (float) context
        .generator()
        .rainfallVarianceNoise.noise(x, z);
    }
  }

  /**
   * Disables temperature modification based on bias and ocean proximity when using Köppen map.
   */
  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/util/Mth;lerp(FFF)F",
      ordinal = 1
    )
  )
  private float tfcrealworld$preserveTemperatureBiasTarget(
    float delta,
    float start,
    float end
  ) {
    if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return end;
    }
    return Mth.lerp(delta, start, end);
  }

  /**
   * Disables temperature modification based on tempDelta and oceanic influence when using Köppen map.
   */
  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/util/Mth;lerp(FFF)F",
      ordinal = 2
    )
  )
  private float tfcrealworld$preserveTemperatureFromMap(
    float delta,
    float start,
    float end
  ) {
    if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return start;
    }
    return Mth.lerp(delta, start, end);
  }

  /**
   * Disables rainfall modification based on bias and ocean proximity when using rainfall map.
   */
  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/util/Mth;lerp(FFF)F",
      ordinal = 3
    )
  )
  private float tfcrealworld$preserveRainfallFromMap(
    float delta,
    float start,
    float end
  ) {
    if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return start;
    }
    return Mth.lerp(delta, start, end);
  }

  /**
   * Disables rainfallVariance reduction at cell edges when using rainVar map.
   */
  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/util/Mth;lerp(FFF)F",
      ordinal = 4
    )
  )
  private float tfcrealworld$preserveRainfallVarianceFromMap(
    float delta,
    float start,
    float end
  ) {
    if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return start;
    }
    return Mth.lerp(delta, start, end);
  }
}
