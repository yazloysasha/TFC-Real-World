package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;

/**
 * Calculator for distance to west coast based on global cache.
 */
public class WestCoastDistanceCalculator extends RegionPointCalculator {

  @Override
  public void calculate(Region region, RegionGenerator generator) {
    if (!isContinentFromMapEnabled()) {
      return;
    }

    final GlobalWestCoastDistanceCache cache =
      GlobalWestCoastDistanceCache.getInstance();
    if (!validateCache(cache)) {
      return;
    }

    forEachPoint(region, point -> {
      point.distanceToWestCoast = cache.getDistance(point.x, point.z);
    });
  }
}
