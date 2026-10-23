package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import com.newterraearth.tfe.world.NTE121ClimateHelpers;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.RainVarianceRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainVarianceNoise;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalWestCoastDistanceCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TerraFirmaEarth works out the distance to the west coast and a chunk's
 * rainfall variance in this helper, not in region tasks.
 */
@Mixin(value = NTE121ClimateHelpers.class, remap = false)
public class TfeClimateHelpersMixin {

  @Inject(method = "getDistanceToWestCoast", at = @At("RETURN"))
  private static void tfcrealworld$westCoastOfTheMap(
    Region region,
    int temperatureScale,
    CallbackInfoReturnable<byte[]> cir
  ) {
    final GlobalWestCoastDistanceCache cache =
      TFCRealWorldConfig.CONTINENT_FROM_MAP.get()
        ? GlobalWestCoastDistanceCache.getInstance()
        : null;
    if (cache == null) {
      return;
    }
    final byte[] distances = cir.getReturnValue();
    for (int index = 0; index < distances.length; index++) {
      distances[index] = cache.getDistance(
        RegionCoords.gridX(region, index),
        RegionCoords.gridZ(region, index)
      );
    }
  }

  @Inject(method = "getPointRainVariance", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$varianceOfTheMap(
    long levelSeed,
    RegionGenerator generator,
    int temperatureScale,
    int gridX,
    int gridZ,
    CallbackInfoReturnable<Float> cir
  ) {
    final PNGRainVarianceNoise variance = RainVarianceRegistry.get(generator);
    if (variance != null) {
      cir.setReturnValue(
        PNGRainVarianceNoise.calendarVariance(
          variance.noise(gridX, gridZ),
          gridZ
        )
      );
    }
  }
}
