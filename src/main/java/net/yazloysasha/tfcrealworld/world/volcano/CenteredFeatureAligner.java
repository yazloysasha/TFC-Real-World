package net.yazloysasha.tfcrealworld.world.volcano;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;

/**
 * Stamp the volcano biome onto the cellular cell center so the cone sits on
 * the map hotspot, matching 1.21.1 {@code CenteredFeatureAligner}.
 *
 * <p>TFC {@code VolcanoNoise} uses {@code Cellular2D(seed).spread(0.009)}.
 */
public final class CenteredFeatureAligner {

  static final int MAX_CENTER_OFFSET_GRID = 2;
  static final int HALF_GRID_BLOCK = TFCRealWorld.GRID_WIDTH_IN_BLOCK / 2;

  private CenteredFeatureAligner() {}

  public static void alignTfc(Region region, long seed) {
    final Cellular2D cells = new Cellular2D(seed).spread(0.009f);
    stamp(
      region,
      (blockX, blockZ) -> {
        final Cellular2D.Cell cell = cells.cell((float) blockX, (float) blockZ);
        return new SampledCell(cell.x(), cell.y());
      },
      MapHotspotBiomes::isTfcVolcanoBiome,
      CenteredFeatureAligner::skipTfcCenter
    );
  }

  static void stamp(
    Region region,
    CellLookup cells,
    BiomePredicate matches,
    CenterFilter skipCenter
  ) {
    final Long2IntOpenHashMap pending = new Long2IntOpenHashMap();
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null || !matches.test(point.biome)) {
        continue;
      }
      final int gridX = RegionCoords.gridX(region, index);
      final int gridZ = RegionCoords.gridZ(region, index);
      final int blockX = RegionCoords.gridToBlock(gridX) + HALF_GRID_BLOCK;
      final int blockZ = RegionCoords.gridToBlock(gridZ) + HALF_GRID_BLOCK;
      final SampledCell cell = cells.cell(blockX, blockZ);
      final int centerGridX = blockToGrid((int) Math.round(cell.x()));
      final int centerGridZ = blockToGrid((int) Math.round(cell.y()));
      if (
        Math.abs(centerGridX - gridX) > MAX_CENTER_OFFSET_GRID ||
        Math.abs(centerGridZ - gridZ) > MAX_CENTER_OFFSET_GRID
      ) {
        continue;
      }
      if (!contains(region, centerGridX, centerGridZ)) {
        continue;
      }
      pending.putIfAbsent(pack(centerGridX, centerGridZ), point.biome);
    }

    for (final var entry : pending.long2IntEntrySet()) {
      final long key = entry.getLongKey();
      final Region.Point center = region.maybeAt(unpackX(key), unpackZ(key));
      if (center == null || matches.test(center.biome)) {
        continue;
      }
      if (skipCenter.skip(center, entry.getIntValue())) {
        continue;
      }
      center.biome = entry.getIntValue();
    }
  }

  private static boolean skipTfcCenter(Region.Point center, int sourceBiome) {
    if (center.lake() || MapHotspotBiomes.isTfcLakeBiome(center.biome)) {
      return true;
    }
    if (!center.land()) {
      return true;
    }
    return (
      sourceBiome == TFCLayers.CANYONS &&
      (center.mountain() || TFCLayers.isMountains(center.biome))
    );
  }

  private static int blockToGrid(int block) {
    return block >> Units.GRID_BITS;
  }

  private static boolean contains(Region region, int x, int z) {
    return (
      x >= region.minX() &&
      x <= region.maxX() &&
      z >= region.minZ() &&
      z <= region.maxZ()
    );
  }

  private static long pack(int x, int z) {
    return ((long) x << 32) | (z & 0xffffffffL);
  }

  private static int unpackX(long key) {
    return (int) (key >> 32);
  }

  private static int unpackZ(long key) {
    return (int) key;
  }

  @FunctionalInterface
  interface BiomePredicate {
    boolean test(int biome);
  }

  @FunctionalInterface
  interface CellLookup {
    SampledCell cell(double blockX, double blockZ);
  }

  @FunctionalInterface
  interface CenterFilter {
    boolean skip(Region.Point center, int sourceBiome);
  }

  record SampledCell(double x, double y) {}
}
