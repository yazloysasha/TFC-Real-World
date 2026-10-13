package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import org.jetbrains.annotations.Nullable;

/**
 * Grid coordinates for TFC {@code Region.Point}, which has no {@code x}/{@code z}
 * fields: they are reconstructed from the region array.
 */
public final class RegionCoords {

  public static final int GRID_WIDTH_IN_BLOCK = 1 << Units.GRID_BITS;

  private RegionCoords() {}

  public static int gridX(Region region, int index) {
    return region.minX() + (index % region.sizeX());
  }

  public static int gridZ(Region region, int index) {
    return region.minZ() + (index / region.sizeX());
  }

  /** The point of the region that owns the grid cell, as TFC's RegionLayer. */
  public static Region.Point pointAt(
    RegionGenerator generator,
    int gridX,
    int gridZ
  ) {
    return generator.getOrCreateRegion(gridX, gridZ).at(gridX, gridZ);
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

  public static int blockToGrid(int block) {
    return block >> Units.GRID_BITS;
  }

  public static int gridToBlock(int grid) {
    return grid * RegionCoords.GRID_WIDTH_IN_BLOCK;
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
