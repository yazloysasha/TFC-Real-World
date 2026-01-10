package net.yazloysasha.tfcrealworld.world.region.calculator;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalOceanDistanceCache;
import org.jetbrains.annotations.Nullable;

/**
 * Calculator for distance to ocean based on global cache.
 * Also correctly identifies shore points for river generation.
 */
public class OceanDistanceCalculator extends RegionPointCalculator {

  @Override
  public void calculate(Region region, RegionGenerator generator) {
    if (!isContinentFromMapEnabled()) {
      return;
    }

    final GlobalOceanDistanceCache cache =
      GlobalOceanDistanceCache.getInstance();
    if (!validateCache(cache)) {
      return;
    }

    forEachPoint(region, point -> {
      point.distanceToOcean = cache.getDistance(point.x, point.z, point.land());
    });

    forEachPoint(region, point -> {
      if (!point.land()) {
        if (hasNonIslandLandNeighbor(region, point)) {
          point.setShore();
        }
      }
    });
  }

  private boolean hasNonIslandLandNeighbor(Region region, Region.Point point) {
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        if (dx == 0 && dz == 0) continue;
        @Nullable
        Region.Point neighbor = region.atOffset(point.index, dx, dz);
        if (neighbor != null && neighbor.land() && !neighbor.island()) {
          return true;
        }
      }
    }
    return false;
  }
}
