package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.Region;
import org.jetbrains.annotations.Nullable;

/**
 * Grid coordinates for TFC 2 {@code Region.Point}, which has no {@code x}/{@code z}
 * fields.
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
