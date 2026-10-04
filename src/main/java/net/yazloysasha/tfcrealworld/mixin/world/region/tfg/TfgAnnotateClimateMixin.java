package net.yazloysasha.tfcrealworld.mixin.world.region.tfg;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.region.MapRegionTasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.terrafirmagreg.core.world.new_ow_wg.region.TFGAnnotateClimate;

/**
 * Climate from the maps as it is. Procedural climate only gets the
 * temperature band shifted by the configured scale.
 */
@Mixin(value = TFGAnnotateClimate.class, remap = false)
public class TfgAnnotateClimateMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$climateFromMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (MapRegionTasks.climate(context)) {
      ci.cancel();
    }
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
