package net.yazloysasha.tfcrealworld.world.volcano;

import static net.dries007.tfc.world.layer.TFCLayers.*;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.volcano.CenteredFeatureNoise;
import net.yazloysasha.tfcrealworld.world.biome.CoverageRareBiomes;

public final class CenteredFeatureAligner {

  private static final int MAX_CENTER_OFFSET_GRID = 2;
  private static final int HALF_GRID_BLOCK = Units.GRID_WIDTH_IN_BLOCK / 2;

  private CenteredFeatureAligner() {}

  public static void align(Region region, long seed) {
    stamp(
      region,
      new Cellular2D(seed, 2).spread(
        CenteredFeatureNoise.STRATOVOLCANO_SPREAD_FACTOR
      ),
      CenteredFeatureAligner::isStratovolcanoBiome
    );
    stamp(
      region,
      new Cellular2D(seed, 0.2f, 1).spread(0.003f),
      CenteredFeatureAligner::isTuffRingBiome
    );
    stamp(
      region,
      new Cellular2D(seed).spread(0.009f),
      CenteredFeatureAligner::isShieldCinderBiome
    );
  }

  private static void stamp(
    Region region,
    Cellular2D cells,
    BiomePredicate matches
  ) {
    final Long2IntOpenHashMap pending = new Long2IntOpenHashMap();
    for (final Region.Point point : region.points()) {
      if (point == null || !matches.test(point.biome)) {
        continue;
      }
      final int blockX = Units.gridToBlock(point.x) + HALF_GRID_BLOCK;
      final int blockZ = Units.gridToBlock(point.z) + HALF_GRID_BLOCK;
      final Cellular2D.Cell cell = cells.cell(blockX, blockZ);
      final int centerGridX = Units.blockToGrid((int) Math.round(cell.x()));
      final int centerGridZ = Units.blockToGrid((int) Math.round(cell.y()));
      if (
        Math.abs(centerGridX - point.x) > MAX_CENTER_OFFSET_GRID ||
        Math.abs(centerGridZ - point.z) > MAX_CENTER_OFFSET_GRID
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
      final Region.Point center = region.at(unpackX(key), unpackZ(key));
      if (center == null || matches.test(center.biome)) {
        continue;
      }
      if (isShieldHotspotBiome(center.biome)) {
        continue;
      }
      if (isLake(center.biome)) {
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

  private static boolean isStratovolcanoBiome(int biome) {
    return (
      biome == OCEANIC_VOLCANIC_ARC ||
      biome == VOLCANIC_MOUNTAINS ||
      biome == VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == VOLCANIC_ISLAND ||
      biome == VOLCANIC_MOUNTAIN_LAKE ||
      biome == VOLCANIC_OCEANIC_MOUNTAIN_LAKE ||
      biome == ICE_SHEET_VOLCANIC_MOUNTAINS ||
      biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
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
      biome == ACTIVE_SHIELD_VOLCANO || biome == VOLCANIC_MOUNTAIN_ISLANDS
    );
  }

  @FunctionalInterface
  private interface BiomePredicate {
    boolean test(int biome);
  }
}
