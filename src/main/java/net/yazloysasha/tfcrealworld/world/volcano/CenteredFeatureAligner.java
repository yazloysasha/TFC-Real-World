package net.yazloysasha.tfcrealworld.world.volcano;

import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.*;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.world.biome.CoverageRareBiomes;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import su.terrafirmagreg.core.world.new_ow_wg.noise.TFGCellular2D;

/**
 * TFG already samples cinder cones and tuff rings from cellular cell centers.
 * Stamp the volcano biome onto that center cell so the cone/ring actually sits
 * on the map hotspot, matching 1.21.1 {@code CenteredFeatureAligner}.
 *
 * <p>TFC 4 stratovolcano cells ({@code Cellular2D(seed, 2)} at 0.0021) are not
 * in TFG — volcanic mountains still use cinder cones — so that stamp is skipped.
 */
public final class CenteredFeatureAligner {

  private static final int MAX_CENTER_OFFSET_GRID = 2;
  private static final int HALF_GRID_BLOCK = Units.GRID_WIDTH_IN_BLOCK / 2;

  private CenteredFeatureAligner() {}

  public static void align(Region region, long seed) {
    stamp(
      region,
      new TFGCellular2D(seed, 0.2f, 1).spread(0.003f),
      CenteredFeatureAligner::isTuffRingBiome
    );
    stamp(
      region,
      new TFGCellular2D(seed).spread(0.009f),
      CenteredFeatureAligner::isShieldCinderBiome
    );
  }

  private static void stamp(
    Region region,
    TFGCellular2D cells,
    BiomePredicate matches
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
      final TFGCellular2D.TFGCell cell = cells.cell(blockX, blockZ);
      final int centerGridX = Units.blockToGrid((int) Math.round(cell.x()));
      final int centerGridZ = Units.blockToGrid((int) Math.round(cell.y()));
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
      if (isShieldHotspotBiome(center.biome)) {
        continue;
      }
      if (isLakeBiome(center.biome) || center.lake()) {
        continue;
      }
      if (CoverageRareBiomes.preserve(center.biome)) {
        continue;
      }
      center.biome = entry.getIntValue();
    }
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

  private static boolean isShieldHotspotBiome(int biome) {
    return (
      biome == ACTIVE_SHIELD_VOLCANO ||
      biome == DORMANT_SHIELD_VOLCANO ||
      biome == EXTINCT_SHIELD_VOLCANO ||
      biome == ANCIENT_SHIELD_VOLCANO ||
      biome == SUNKEN_SHIELD_VOLCANO ||
      biome == ICE_SHEET_SHIELD_VOLCANO ||
      biome == GLACIATED_SHIELD_VOLCANO ||
      biome == SHIELD_VOLCANO_SHORE ||
      biome == OLD_SHIELD_VOLCANO_SHORE
    );
  }

  private static boolean isTuffRingBiome(int biome) {
    return (
      biome == DORMANT_SHIELD_VOLCANO ||
      biome == EXTINCT_SHIELD_VOLCANO ||
      biome == ANCIENT_SHIELD_VOLCANO ||
      biome == SUNKEN_SHIELD_VOLCANO ||
      biome == OLD_SHIELD_VOLCANO_SHORE
    );
  }

  private static boolean isShieldCinderBiome(int biome) {
    return (
      biome == ACTIVE_SHIELD_VOLCANO ||
      biome == VOLCANIC_MOUNTAINS ||
      biome == VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == VOLCANIC_MOUNTAIN_LAKE ||
      biome == VOLCANIC_OCEANIC_MOUNTAIN_LAKE
    );
  }

  private static boolean isLakeBiome(int biome) {
    return (
      biome == LAKE ||
      biome == MOUNTAIN_LAKE ||
      biome == OLD_MOUNTAIN_LAKE ||
      biome == OCEANIC_MOUNTAIN_LAKE ||
      biome == VOLCANIC_MOUNTAIN_LAKE ||
      biome == VOLCANIC_OCEANIC_MOUNTAIN_LAKE ||
      biome == PLATEAU_LAKE ||
      biome == SUBGLACIAL_LAKE ||
      biome == MELTWATER_LAKE ||
      biome == TOWER_KARST_LAKE
    );
  }

  @FunctionalInterface
  private interface BiomePredicate {
    boolean test(int biome);
  }
}
