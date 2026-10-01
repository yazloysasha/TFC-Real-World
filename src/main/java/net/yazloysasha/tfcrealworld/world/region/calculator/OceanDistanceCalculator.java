package net.yazloysasha.tfcrealworld.world.region.calculator;

import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalOceanDistanceCache;
import org.jetbrains.annotations.Nullable;

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
    for (final Region.Point point : region.points()) {
      if (point != null) {
        point.distanceToOcean = cache.getDistance(
          point.x,
          point.z,
          point.land()
        );
      }
    }
    for (final Region.Point point : region.points()) {
      if (
        point != null && !point.land() && hasMainlandNeighbour(region, point)
      ) {
        point.setShore();
      }
    }
  }

  private static boolean hasMainlandNeighbour(
    Region region,
    Region.Point point
  ) {
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        if (dx == 0 && dz == 0) {
          continue;
        }
        final @Nullable Region.Point neighbour = region.atOffset(
          point.index,
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
