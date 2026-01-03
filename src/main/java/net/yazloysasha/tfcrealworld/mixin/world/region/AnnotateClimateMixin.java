package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateClimate;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AnnotateClimate.class)
public class AnnotateClimateMixin {

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
  private void realworld$overrideRainfallVariance(
    RegionGenerator.Context context,
    CallbackInfo ci,
    java.util.Iterator<?> iterator,
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
  @org.spongepowered.asm.mixin.injection.Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/util/Mth;lerp(FFF)F",
      ordinal = 1
    )
  )
  private float realworld$preserveTemperatureBiasTarget(
    float delta,
    float start,
    float end
  ) {
    if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return end;
    }
    return net.minecraft.util.Mth.lerp(delta, start, end);
  }

  /**
   * Disables temperature modification based on tempDelta and oceanic influence when using Köppen map.
   */
  @org.spongepowered.asm.mixin.injection.Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/util/Mth;lerp(FFF)F",
      ordinal = 2
    )
  )
  private float realworld$preserveTemperatureFromMap(
    float delta,
    float start,
    float end
  ) {
    if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return start;
    }
    return net.minecraft.util.Mth.lerp(delta, start, end);
  }

  /**
   * Disables rainfall modification based on bias and ocean proximity when using rainfall map.
   */
  @org.spongepowered.asm.mixin.injection.Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/util/Mth;lerp(FFF)F",
      ordinal = 3
    )
  )
  private float realworld$preserveRainfallFromMap(
    float delta,
    float start,
    float end
  ) {
    if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return start;
    }
    return net.minecraft.util.Mth.lerp(delta, start, end);
  }

  /**
   * Disables rainfallVariance reduction at cell edges when using rainVar map.
   */
  @org.spongepowered.asm.mixin.injection.Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/util/Mth;lerp(FFF)F",
      ordinal = 4
    )
  )
  private float realworld$preserveRainfallVarianceFromMap(
    float delta,
    float start,
    float end
  ) {
    if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return start;
    }
    return net.minecraft.util.Mth.lerp(delta, start, end);
  }
}
