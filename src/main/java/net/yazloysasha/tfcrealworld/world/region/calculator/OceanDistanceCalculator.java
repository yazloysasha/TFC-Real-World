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

    final int minX = region.minX();
    final int minZ = region.minZ();
    final int sizeX = region.sizeX();

    forEachPoint(region, point -> {
      final int index = findPointIndex(region.data(), point);
      if (index == -1) return;

      final int localX = index % sizeX;
      final int localZ = index / sizeX;
      final int gridX = minX + localX;
      final int gridZ = minZ + localZ;

      point.distanceToOcean = cache.getDistance(gridX, gridZ, point.land());
    });

    forEachPoint(region, point -> {
      if (!point.land()) {
        if (hasNonIslandLandNeighbor(region, point)) {
          point.setShore();
        }
      }
    });
  }

  private int findPointIndex(Region.Point[] data, Region.Point point) {
    for (int i = 0; i < data.length; i++) {
      if (data[i] == point) {
        return i;
      }
    }
    return -1;
  }

  private boolean hasNonIslandLandNeighbor(Region region, Region.Point point) {
    final int pointIndex = findPointIndex(region.data(), point);
    if (pointIndex == -1) return false;

    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        if (dx == 0 && dz == 0) continue;

        final int neighborIndex = region.offset(pointIndex, dx, dz);
        if (neighborIndex == -1) continue;

        @Nullable
        Region.Point neighbor = region.data()[neighborIndex];
        if (neighbor != null && neighbor.land() && !neighbor.island()) {
          return true;
        }
      }
    }
    return false;
  }
}
