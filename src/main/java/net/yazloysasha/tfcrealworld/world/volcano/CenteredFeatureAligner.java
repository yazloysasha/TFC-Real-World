package net.yazloysasha.tfcrealworld.world.volcano;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.function.IntPredicate;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.Volcanism;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;

/** Snaps cellular cone centers onto a matching volcanic biome cell. */
public final class CenteredFeatureAligner {

  private static final int MAX_CENTER_OFFSET_GRID = 2;
  private static final int HALF_GRID_BLOCK =
    RegionCoords.GRID_WIDTH_IN_BLOCK / 2;

  /** The centre, in blocks, of the cone cell a block lies in. */
  @FunctionalInterface
  public interface CellLookup {
    double[] centerOf(int blockX, int blockZ);
  }

  private CenteredFeatureAligner() {}

  /**
   * @param matches biomes that build the cone of these cells
   */
  public static void stamp(
    Region region,
    TectonicsMap tectonics,
    CellLookup cells,
    IntPredicate matches
  ) {
    final Region.Point[] data = region.data();
    final Long2IntOpenHashMap pending = new Long2IntOpenHashMap();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null || !matches.test(point.biome)) {
        continue;
      }
      final int x = RegionCoords.gridX(region, index);
      final int z = RegionCoords.gridZ(region, index);
      final double[] center = cells.centerOf(
        RegionCoords.gridToBlock(x) + HALF_GRID_BLOCK,
        RegionCoords.gridToBlock(z) + HALF_GRID_BLOCK
      );
      final int centerX = RegionCoords.blockToGrid((int) Math.round(center[0]));
      final int centerZ = RegionCoords.blockToGrid((int) Math.round(center[1]));
      if (
        Math.abs(centerX - x) > MAX_CENTER_OFFSET_GRID ||
        Math.abs(centerZ - z) > MAX_CENTER_OFFSET_GRID ||
        !region.isIn(centerX, centerZ)
      ) {
        continue;
      }
      pending.putIfAbsent(pack(centerX, centerZ), point.biome);
    }
    for (final var entry : pending.long2IntEntrySet()) {
      final int x = (int) (entry.getLongKey() >> 32);
      final int z = (int) entry.getLongKey();
      final Region.Point center = region.at(x, z);
      // A cone moves only within volcanic land: never onto the sea or onto
      // a cell the tectonics map left non-volcanic.
      if (
        center == null ||
        !center.land() ||
        center.lake() ||
        matches.test(center.biome) ||
        TFCLayers.isLake(center.biome)
      ) {
        continue;
      }
      if (tectonics.classAtGrid(x, z).volcanism() == Volcanism.NONE) {
        continue;
      }
      center.biome = entry.getIntValue();
    }
  }

  private static long pack(int x, int z) {
    return ((long) x << 32) | (z & 0xffffffffL);
  }
}
