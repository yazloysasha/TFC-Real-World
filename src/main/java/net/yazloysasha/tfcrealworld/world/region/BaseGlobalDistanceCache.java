package net.yazloysasha.tfcrealworld.world.region;

import com.mojang.logging.LogUtils;
import net.yazloysasha.tfcrealworld.world.noise.PNGContinentNoise;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Base class for global distance caches that share common singleton functionality.
 * Provides common initialization, clearing, and instance management.
 */
abstract class BaseGlobalDistanceCache extends BaseDistanceCache {

  protected static final Logger LOGGER = LogUtils.getLogger();

  protected BaseGlobalDistanceCache(PNGContinentNoise continentNoise) {
    super(continentNoise);
  }

  protected static <T extends BaseGlobalDistanceCache> T initializeInstance(
    @Nullable T currentInstance,
    T newInstance,
    String cacheName
  ) {
    if (currentInstance == null) {
      LOGGER.info(
        "Global {} cache initialized: {}x{}",
        cacheName,
        newInstance.width,
        newInstance.height
      );
      return newInstance;
    }
    return currentInstance;
  }

  protected static <T extends BaseGlobalDistanceCache> void clearInstance(
    @Nullable T instance,
    String cacheName
  ) {
    if (instance != null) {
      LOGGER.info("Clearing global {} cache", cacheName);
    }
  }

  protected byte[] getDistanceValues(InterpolationResult interpolation) {
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

  protected byte interpolateDistance(InterpolationResult interpolation) {
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
