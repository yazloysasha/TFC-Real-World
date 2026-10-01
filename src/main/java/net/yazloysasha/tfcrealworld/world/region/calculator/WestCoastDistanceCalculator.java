package net.yazloysasha.tfcrealworld.world.region.calculator;

import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalWestCoastDistanceCache;

/** Distance to the map's west coasts for every point of a region. */
public final class WestCoastDistanceCalculator {

  private WestCoastDistanceCalculator() {}

  public static void apply(Region region) {
    final GlobalWestCoastDistanceCache cache =
      GlobalWestCoastDistanceCache.getInstance();
    if (cache == null) {
      return;
    }
    for (final Region.Point point : region.points()) {
      if (point != null) {
        point.distanceToWestCoast = cache.getDistance(point.x, point.z);
      }
    }
  }
}
