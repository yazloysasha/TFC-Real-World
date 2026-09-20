package net.yazloysasha.tfcrealworld.world.volcano;

import java.util.function.BiConsumer;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;

public final class MapHotspotApplicator {

  private MapHotspotApplicator() {}

  public static void applyAgesAndLand(
    Region region,
    MapHotspotLayout layout,
    BiConsumer<Region.Point, Byte> setAge
  ) {
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      final byte mapAge = layout.ageAtGrid(
        RegionCoords.gridX(region, index),
        RegionCoords.gridZ(region, index)
      );
      if (mapAge > 0) {
        setAge.accept(point, mapAge);
        if (MapHotspotBiomes.shouldSetLandForMapAge(mapAge)) {
          point.setLand();
        }
      }
    }
  }
}
