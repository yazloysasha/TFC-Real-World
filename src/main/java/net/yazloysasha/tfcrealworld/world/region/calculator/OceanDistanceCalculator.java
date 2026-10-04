package net.yazloysasha.tfcrealworld.world.region.calculator;

import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalOceanDistanceCache;

/**
 * Distance to the map's ocean for every point of a region, and the shore
 * flag river generation starts from (sea next to non-island land).
 */
public final class OceanDistanceCalculator {

  private OceanDistanceCalculator() {}

  public static void apply(Region region) {
    final GlobalOceanDistanceCache cache =
      GlobalOceanDistanceCache.getInstance();
    if (cache == null) {
      return;
    }
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point != null) {
        point.distanceToOcean = cache.getDistance(
          RegionCoords.gridX(region, index),
          RegionCoords.gridZ(region, index),
          point.land()
        );
      }
    }
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (
        point != null && !point.land() && hasMainlandNeighbour(region, index)
      ) {
        point.setShore();
      }
    }
  }

  private static boolean hasMainlandNeighbour(Region region, int index) {
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        final Region.Point neighbour = RegionCoords.atOffset(
          region,
          index,
          dx,
          dz
        );
        if (neighbour != null && neighbour.land() && !neighbour.island()) {
          return true;
        }
      }
    }
    return false;
  }
}
