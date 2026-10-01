package net.yazloysasha.tfcrealworld.world;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.mixin.world.layer.TFCLayersAccessor;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise.ContinentBand;
import org.jetbrains.annotations.Nullable;

/**
 * Salt vs fresh water for map lakes.
 * <p>
 * A map lake keeps the vanilla {@code lakeFor} biome, so salinity has to come
 * from the continent map. It is decided per <b>biome cell</b>: the same
 * 16-block cells, sampled at the same point, that
 * {@code MapLandOceanCorrectionLayer} uses to paint the lake biome footprint.
 * A lake cell is therefore always either fresh or salt, never "unknown".
 * <p>
 * TFC blends biomes over a wider area than one cell, so a water column can
 * belong to several cells. Its salinity is a distance-weighted vote of the
 * lake cells around it. This follows the blended shoreline instead of the
 * 8-block pixels of {@code continent.png}.
 * <p>
 * Only {@code isSalty()} of a lake biome is overridden (see
 * {@code BiomeExtensionMixin}). Oceans and everything else, including TFC's
 * own coastal salt-water rule, stay vanilla.
 */
public final class MapLakeWater {

  public enum Salinity {
    FRESH,
    SALT,
  }

  /**
   * Biome cell size in blocks, as a shift. Must match the layer where
   * {@code MapLandOceanCorrectionLayer} runs (grid, then 3 zooms = 16 blocks).
   */
  private static final int CELL_BITS = Units.QUART_BITS + 2;

  /**
   * Same value as {@code MapLandOceanCorrectionLayer#layerToGrid}.
   */
  private static final double CELL_TO_GRID =
    1.0 / (1 << (Units.GRID_BITS - CELL_BITS));

  private static final int CELL_SIZE = 1 << CELL_BITS;

  /**
   * Cells searched around the column. TFC's widest biome blend reaches about
   * 64 blocks, but the weight fades quickly; 3 cells covers the visible part.
   */
  private static final int SEARCH_RADIUS_CELLS = 3;

  /**
   * Vote weight is {@code 1 / (1 + (distance / falloff)^2)}.
   */
  private static final double FALLOFF_BLOCKS = CELL_SIZE;

  private static final class Column {

    boolean active;
    int blockX;
    int blockZ;
    boolean resolved;

    @Nullable
    Salinity salinity;
  }

  /**
   * Column being sampled on this thread. {@code isSalty()} is asked several
   * times per column, so the vote is computed once, on first use.
   */
  private static final ThreadLocal<Column> COLUMN = ThreadLocal.withInitial(
    Column::new
  );

  private static volatile Set<BiomeExtension> lakeBiomes;

  private MapLakeWater() {}

  public static void enterColumn(int blockX, int blockZ) {
    final Column column = COLUMN.get();
    column.active = true;
    column.blockX = blockX;
    column.blockZ = blockZ;
    column.resolved = false;
    column.salinity = null;
  }

  public static void leaveColumn() {
    COLUMN.get().active = false;
  }

  /**
   * @return true while this thread is sampling a column.
   */
  public static boolean isColumnOpen() {
    return COLUMN.get().active;
  }

  /**
   * @return true for every biome TFC itself treats as a lake
   * ({@link TFCLayers#isLake}), so no biome is listed here and addon lake
   * biomes registered through TFCLayers are covered too.
   */
  public static boolean isLakeBiome(BiomeExtension biome) {
    Set<BiomeExtension> set = lakeBiomes;
    if (set == null) {
      set = Collections.newSetFromMap(new IdentityHashMap<>());
      final BiomeExtension[] layers =
        TFCLayersAccessor.tfcrealworld$getBiomeLayers();
      for (int id = 0; id < layers.length; id++) {
        if (layers[id] != null && TFCLayers.isLake(id)) {
          set.add(layers[id]);
        }
      }
      lakeBiomes = set;
    }
    return set.contains(biome);
  }

  /**
   * @return what a lake biome's {@code isSalty()} should answer for the open
   * column, or {@code null} to keep vanilla (no column, or no map lake nearby).
   */
  @Nullable
  public static Boolean saltyFlagOverride() {
    final Column column = COLUMN.get();
    if (!column.active) {
      return null;
    }
    if (!column.resolved) {
      column.salinity = atBlock(column.blockX, column.blockZ);
      column.resolved = true;
    }
    return column.salinity == null ? null : column.salinity == Salinity.SALT;
  }

  /**
   * Salinity of the map lake water around a block: a distance-weighted vote of
   * the lake cells within {@link #SEARCH_RADIUS_CELLS}.
   *
   * @return {@code null} when no map lake cell is close (vanilla salinity stays).
   */
  @Nullable
  public static Salinity atBlock(int blockX, int blockZ) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return null;
    }
    final PNGContinentNoise noise = ContinentNoiseRegistry.any();
    if (noise == null) {
      return null;
    }

    final int cellX = blockX >> CELL_BITS;
    final int cellZ = blockZ >> CELL_BITS;
    final double px = blockX + 0.5;
    final double pz = blockZ + 0.5;

    double fresh = 0.0;
    double salt = 0.0;
    for (int dz = -SEARCH_RADIUS_CELLS; dz <= SEARCH_RADIUS_CELLS; dz++) {
      for (int dx = -SEARCH_RADIUS_CELLS; dx <= SEARCH_RADIUS_CELLS; dx++) {
        final int cx = cellX + dx;
        final int cz = cellZ + dz;
        final ContinentBand band = noise.bandAtGridHard(
          cx * CELL_TO_GRID,
          cz * CELL_TO_GRID
        );
        if (band != ContinentBand.LAKE && band != ContinentBand.SALT_LAKE) {
          continue;
        }
        final double distX = px - ((cx << CELL_BITS) + CELL_SIZE * 0.5);
        final double distZ = pz - ((cz << CELL_BITS) + CELL_SIZE * 0.5);
        final double d =
          Math.sqrt(distX * distX + distZ * distZ) / FALLOFF_BLOCKS;
        final double weight = 1.0 / (1.0 + d * d);
        if (band == ContinentBand.SALT_LAKE) {
          salt += weight;
        } else {
          fresh += weight;
        }
      }
    }

    if (salt == 0.0 && fresh == 0.0) {
      return null;
    }
    return salt > fresh ? Salinity.SALT : Salinity.FRESH;
  }
}
