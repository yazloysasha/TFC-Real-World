package net.yazloysasha.tfcrealworld.world.backport;

import java.util.function.BiConsumer;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalWestCoastDistanceCache;
import org.jetbrains.annotations.Nullable;

/**
 * Continent-PNG west-coast distances for TFG/TFE point fields and TFE buffers.
 */
public final class WestCoastFromMap {

  private WestCoastFromMap() {}

  public static boolean apply(
    Region region,
    BiConsumer<Region.Point, Byte> setDistance
  ) {
    final GlobalWestCoastDistanceCache cache = mapCache();
    if (cache == null) {
      return false;
    }
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      setDistance.accept(
        point,
        cache.getDistance(
          RegionCoords.gridX(region, index),
          RegionCoords.gridZ(region, index)
        )
      );
    }
    return true;
  }

  /** Same indexing as {@code Region.data()} / {@code Region.index}. */
  public static boolean fill(Region region, byte[] distances) {
    if (distances == null) {
      return false;
    }
    final GlobalWestCoastDistanceCache cache = mapCache();
    if (cache == null) {
      return false;
    }
    final Region.Point[] data = region.data();
    if (distances.length != data.length) {
      return false;
    }
    for (int index = 0; index < data.length; index++) {
      if (data[index] == null) {
        continue;
      }
      distances[index] = cache.getDistance(
        RegionCoords.gridX(region, index),
        RegionCoords.gridZ(region, index)
      );
    }
    return true;
  }

  @Nullable
  private static GlobalWestCoastDistanceCache mapCache() {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return null;
    }
    return GlobalWestCoastDistanceCache.getInstance();
  }
}
