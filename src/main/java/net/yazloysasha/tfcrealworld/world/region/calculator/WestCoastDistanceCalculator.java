package net.yazloysasha.tfcrealworld.world.region.calculator;

import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.backend.WorldBackend;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalWestCoastDistanceCache;

/** Distance to the map's west coasts for every point of a region. */
public final class WestCoastDistanceCalculator {

  private WestCoastDistanceCalculator() {}

  public static void apply(Region region, WorldBackend backend) {
    final GlobalWestCoastDistanceCache cache =
      GlobalWestCoastDistanceCache.getInstance();
    if (cache == null) {
      return;
    }
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      if (data[index] != null) {
        backend.setDistanceToWestCoast(
          data[index],
          cache.getDistance(
            RegionCoords.gridX(region, index),
            RegionCoords.gridZ(region, index)
          )
        );
      }
    }
  }
}
