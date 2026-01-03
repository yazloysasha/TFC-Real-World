package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.jetbrains.annotations.Nullable;

/**
 * Calculator for distance to ocean based on global cache.
 * Also correctly identifies shore points for river generation.
 */
public class OceanDistanceCalculator {

  public static void calculateDistanceToOcean(
    Region region,
    RegionGenerator generator
  ) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return;
    }

    GlobalOceanDistanceCache cache = GlobalOceanDistanceCache.getInstance();
    if (cache == null) {
      return;
    }

    for (final var point : region.points()) {
      if (point != null) {
        point.distanceToOcean = cache.getDistance(
          point.x,
          point.z,
          point.land()
        );
      }
    }

    for (final var point : region.points()) {
      if (point != null && !point.land()) {
        boolean hasNonIslandLandNeighbor = false;

        for (int dx = -1; dx <= 1; dx++) {
          for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dz == 0) continue;

            @Nullable
            Region.Point neighbor = region.atOffset(point.index, dx, dz);
            if (neighbor != null && neighbor.land() && !neighbor.island()) {
              hasNonIslandLandNeighbor = true;
              break;
            }
          }
          if (hasNonIslandLandNeighbor) break;
        }

        if (hasNonIslandLandNeighbor) {
          point.setShore();
        }
      }
    }
  }
}
