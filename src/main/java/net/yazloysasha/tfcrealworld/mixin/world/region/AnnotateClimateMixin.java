package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.AnnotateClimate;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.region.MapRegionTasks;
import net.yazloysasha.tfcrealworld.world.region.RegionContext;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Climate from the maps as it is. Procedural climate only gets the
 * temperature band shifted by the configured scale.
 */
@Mixin(value = AnnotateClimate.class, remap = false, priority = 1500)
public class AnnotateClimateMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$climateFromMap(CallbackInfo ci) {
    final RegionContext context = RegionContext.current();
    if (MapRegionTasks.climate(context)) {
      ci.cancel();
    }
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/noise/Noise2D;noise(FF)F",
      ordinal = 0
    )
  )
  private float tfcrealworld$shiftTemperatureBand(
    Noise2D instance,
    float x,
    float z
  ) {
    final int temperatureScale = TFCRealWorldConfig.TEMPERATURE_SCALE.get();
    if (temperatureScale > 0) {
      final float offsetInGrid =
        (float) (-temperatureScale / 2) / RegionCoords.GRID_WIDTH_IN_BLOCK;
      return instance.noise(x, z - offsetInGrid);
    }
    return instance.noise(x, z);
  }
}
