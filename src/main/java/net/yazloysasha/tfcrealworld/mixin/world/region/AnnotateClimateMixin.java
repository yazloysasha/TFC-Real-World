package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.AnnotateClimate;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AnnotateClimate.class, remap = false)
public class AnnotateClimateMixin {

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/noise/Noise2D;noise(FF)F",
      ordinal = 0
    )
  )
  private float tfcrealworld$transformZForTemperature(
    Noise2D instance,
    float x,
    float z
  ) {
    int temperatureScale = TFCRealWorldConfig.TEMPERATURE_SCALE.get();
    if (temperatureScale > 0) {
      float offsetInGrid =
        (float) (-temperatureScale / 2) / TFCRealWorld.GRID_WIDTH_IN_BLOCK;
      return instance.noise(x, z - offsetInGrid);
    }

    return instance.noise(x, z);
  }
}
