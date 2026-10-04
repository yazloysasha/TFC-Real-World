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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TerraFirmaEarth works out the distance to the west coast and a chunk's
 * rainfall variance in this helper, not in region tasks.
 */
@Mixin(value = NTE121ClimateHelpers.class, remap = false)
public class TfeClimateHelpersMixin {

  @Unique
  private static final String POINT_VARIANCE =
    "Lcom/newterraearth/tfe/world/NTE121ClimateHelpers;getPointRainVariance(JLnet/dries007/tfc/world/region/RegionGenerator;III)F";

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

  /**
   * A chunk's variance is blended from the four region points around it.
   * With climate maps a point south of the equator holds the opposite sign
   * of the same climate north of it, so a chunk next to the equator reads
   * the points across it with the sign of its own hemisphere.
   */
  @Redirect(
    method = "getRainVarianceLayer",
    at = @At(value = "INVOKE", target = POINT_VARIANCE, ordinal = 1)
  )
  private static float tfcrealworld$variance01(
    long levelSeed,
    RegionGenerator generator,
    int temperatureScale,
    int gridX,
    int gridZ
  ) {
    return tfcrealworld$inChunkHemisphere(
      levelSeed,
      generator,
      temperatureScale,
      gridX,
      gridZ
    );
  }

  @Redirect(
    method = "getRainVarianceLayer",
    at = @At(value = "INVOKE", target = POINT_VARIANCE, ordinal = 3)
  )
  private static float tfcrealworld$variance11(
    long levelSeed,
    RegionGenerator generator,
    int temperatureScale,
    int gridX,
    int gridZ
  ) {
    return tfcrealworld$inChunkHemisphere(
      levelSeed,
      generator,
      temperatureScale,
      gridX,
      gridZ
    );
  }

  /** The variance of the point one cell south of the chunk's own. */
  @Unique
  private static float tfcrealworld$inChunkHemisphere(
    long levelSeed,
    RegionGenerator generator,
    int temperatureScale,
    int gridX,
    int gridZ
  ) {
    final float variance =
      TfeClimateHelpersInvoker.tfcrealworld$invokeGetPointRainVariance(
        levelSeed,
        generator,
        temperatureScale,
        gridX,
        gridZ
      );
    if (RainVarianceRegistry.get(generator) == null) {
      return variance;
    }
    return (
        PNGRainVarianceNoise.isNorth(gridZ - 1) ==
        PNGRainVarianceNoise.isNorth(gridZ)
      )
      ? variance
      : -variance;
  }
}
