package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

/**
 * Calculator for distance to west coast based on global cache.
 */
public class WestCoastDistanceCalculator {

  public static void calculateDistanceToWestCoast(
    Region region,
    RegionGenerator generator
  ) {
    if (!TFCRealWorldConfig.getContinentFromMap()) {
      return;
    }

    GlobalWestCoastDistanceCache cache =
      GlobalWestCoastDistanceCache.getInstance();
    if (cache == null) {
      return;
    }

    for (final var point : region.points()) {
      if (point != null) {
        point.distanceToWestCoast = cache.getDistance(point.x, point.z);
      }
    }
  }
}
