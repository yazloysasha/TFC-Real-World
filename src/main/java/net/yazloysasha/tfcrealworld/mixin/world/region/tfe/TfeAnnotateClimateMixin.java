package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import com.newterraearth.tfe.world.region.NTEPointAccess;
import com.newterraearth.tfe.world.region.NTERegionGeneratorAccess;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.AnnotateClimate;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.backport.WestCoastFromMap;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.1 AnnotateClimate map hooks on TFE's overwrite. Lerp ordinals differ
 * between TFC 3 and TFE, so map climate is restored at TAIL from the already
 * replaced noises instead of redirecting {@code Mth.lerp}.
 */
@Mixin(value = AnnotateClimate.class, remap = false, priority = 1500)
public class TfeAnnotateClimateMixin {

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/noise/Noise2D;noise(DD)D",
      ordinal = 0
    )
  )
  private double tfcrealworld$tfeTransformZForTemperature(
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

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$tfeOverrideClimateFromMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    final Region region = context.region;
    if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      final RegionGenerator generator = context.generator();
      final NTERegionGeneratorAccess generatorAccess =
        (NTERegionGeneratorAccess) generator;
      final Region.Point[] data = region.data();
      for (int index = 0; index < data.length; index++) {
        final Region.Point point = data[index];
        if (point == null) {
          continue;
        }
        final int x = RegionCoords.gridX(region, index);
        final int z = RegionCoords.gridZ(region, index);
        point.temperature = (float) generator.temperatureNoise.noise(x, z);
        point.rainfall = (float) generator.rainfallNoise.noise(x, z);
        ((NTEPointAccess) point).nte$setRainfallVariance(
            Mth.clamp(
              (float) generatorAccess
                .nte$getRainfallVarianceNoise()
                .noise(x, z),
              -1f,
              1f
            )
          );
      }
    }
    WestCoastFromMap.apply(region, (point, dist) ->
      ((NTEPointAccess) point).nte$setDistanceToWestCoast(dist)
    );
  }
}
