package net.yazloysasha.tfcrealworld.world.region.cache;

import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;

/**
 * A distance map over the whole continent map, built once per map. A new
 * world reuses it when its profile and scale are the same.
 */
abstract class BaseGlobalDistanceCache extends BaseDistanceCache {

  private final String source;

  protected BaseGlobalDistanceCache(PNGContinentNoise continentNoise) {
    super(continentNoise);
    this.source = sourceOf(continentNoise);
  }

  protected boolean isBuiltFrom(PNGContinentNoise continentNoise) {
    return source.equals(sourceOf(continentNoise));
  }

  private static String sourceOf(PNGContinentNoise continentNoise) {
    return (
      TFCRealWorldConfig.MAP_PROFILE.get() +
      ":" +
      continentNoise.getTileRadiusBlocksX() +
      "x" +
      continentNoise.getTileRadiusBlocksZ()
    );
  }

  protected byte[] getDistanceValues(
    BaseDistanceCache.InterpolationResult interpolation
  ) {
    int idx00 = interpolation.z0 * width + interpolation.x0;
    int idx10 = interpolation.z0 * width + interpolation.x1;
    int idx01 = interpolation.z1 * width + interpolation.x0;
    int idx11 = interpolation.z1 * width + interpolation.x1;

    return new byte[] {
      distanceMap[idx00],
      distanceMap[idx10],
      distanceMap[idx01],
      distanceMap[idx11],
    };
  }

  protected byte interpolateDistance(
    BaseDistanceCache.InterpolationResult interpolation
  ) {
    byte[] distances = getDistanceValues(interpolation);
    byte dist00 = distances[0];
    byte dist10 = distances[1];
    byte dist01 = distances[2];
    byte dist11 = distances[3];

    double dist0 = dist00 * (1 - interpolation.fx) + dist10 * interpolation.fx;
    double dist1 = dist01 * (1 - interpolation.fx) + dist11 * interpolation.fx;
    double finalDist =
      dist0 * (1 - interpolation.fz) + dist1 * interpolation.fz;
    return (byte) Math.max(0, Math.round(finalDist));
  }
}
