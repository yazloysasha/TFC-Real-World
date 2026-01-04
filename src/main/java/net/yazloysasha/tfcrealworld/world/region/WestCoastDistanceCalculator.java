package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

/**
 * Calculator for distance to west coast based on global cache.
 */
public class WestCoastDistanceCalculator extends RegionPointCalculator {

  @Override
  public void calculate(Region region, RegionGenerator generator) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return;
    }

    final GlobalWestCoastDistanceCache cache =
      GlobalWestCoastDistanceCache.getInstance();
    if (cache == null) {
      return;
    }

    forEachPoint(
      region,
      point -> {
        point.distanceToWestCoast = cache.getDistance(point.x, point.z);
      }
    );
  }
}
