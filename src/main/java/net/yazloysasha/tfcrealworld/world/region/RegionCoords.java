package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.Units;
import org.jetbrains.annotations.Nullable;

/**
 * Grid coordinates for TFC 3 {@code Region.Point}, which has no {@code x}/{@code z}
 * fields. TFG stores the same values on {@code IRegionPoint}; reconstructing from
 * the region array keeps both pipelines on one helper.
 */
public final class RegionCoords {

  private RegionCoords() {}

  public static int gridX(Region region, int index) {
    return region.minX() + (index % region.sizeX());
  }

  public static int gridZ(Region region, int index) {
    return region.minZ() + (index / region.sizeX());
  }

  public static int indexOf(Region region, Region.Point point) {
    final Region.Point[] data = region.data();
    for (int i = 0; i < data.length; i++) {
      if (data[i] == point) {
        return i;
      }
    }
    return -1;
  }

  public static int gridToBlock(int grid) {
    return grid * Units.GRID_WIDTH_IN_BLOCK;
  }

  @Nullable
  public static Region.Point atOffset(
    Region region,
    int index,
    int dx,
    int dz
  ) {
    final int localX = dx + (index % region.sizeX());
    final int localZ = dz + (index / region.sizeX());
    if (
      localX < 0 ||
      localX >= region.sizeX() ||
      localZ < 0 ||
      localZ >= region.sizeZ()
    ) {
      return null;
    }
    return region.data()[localX + region.sizeX() * localZ];
  }
}
