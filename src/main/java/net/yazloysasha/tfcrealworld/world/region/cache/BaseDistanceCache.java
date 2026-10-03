package net.yazloysasha.tfcrealworld.world.region.cache;

import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;

/**
 * A distance of every region cell of the map, in cells as vanilla counts it.
 * Vanilla measures within one region; the map's land runs across regions, so
 * the distance is measured over the whole map once. A new world reuses it
 * when its profile and scale are the same.
 */
abstract class BaseDistanceCache {

  /** Cells of the map, row by row from its north-west corner. */
  protected final byte[] distances;
  protected final boolean[] land;
  protected final int width;
  protected final int height;

  private final double halfX;
  private final double halfZ;
  private final String source;

  protected BaseDistanceCache(PNGContinentNoise continent) {
    this.halfX = continent.getTileRadiusGridX();
    this.halfZ = continent.getTileRadiusGridZ();
    this.width = Math.max(1, Mth.ceil(2 * halfX));
    this.height = Math.max(1, Mth.ceil(2 * halfZ));
    this.source = sourceOf(continent);
    this.distances = new byte[width * height];
    this.land = new boolean[width * height];
    for (int z = 0; z < height; z++) {
      for (int x = 0; x < width; x++) {
        // The centre of the cell, as a region point takes its land.
        land[z * width + x] = continent.isLandAtGridHard(
          x - halfX + 0.5,
          z - halfZ + 0.5
        );
      }
    }
  }

  protected boolean isBuiltFrom(PNGContinentNoise continent) {
    return source.equals(sourceOf(continent));
  }

  private static String sourceOf(PNGContinentNoise continent) {
    return (
      TFCRealWorldConfig.MAP_PROFILE.get() +
      ":" +
      continent.getTileRadiusBlocksX() +
      "x" +
      continent.getTileRadiusBlocksZ() +
      ":" +
      TFCRealWorldConfig.LAKES_FROM_MAP.get()
    );
  }

  /** The distance of a region point, on the copies of the map as on it. */
  protected byte distanceAt(int gridX, int gridZ) {
    // Fold the cell's centre into the map as BasePNGNoise folds a position.
    final int tileX = Mth.floor((gridX + 0.5 + halfX) / (2 * halfX));
    final int tileZ = Mth.floor((gridZ + 0.5 + halfZ) / (2 * halfZ));
    double localX = gridX + 0.5 - tileX * 2 * halfX;
    double localZ = gridZ + 0.5 - tileZ * 2 * halfZ;
    if (((tileX + tileZ) & 1) != 0) {
      localX = -localX;
    }
    if ((tileZ & 1) != 0) {
      localZ = -localZ;
    }
    final int x = Math.clamp(Mth.floor(localX + halfX), 0, width - 1);
    final int z = Math.clamp(Mth.floor(localZ + halfZ), 0, height - 1);
    return distances[z * width + x];
  }

  protected boolean isLand(int x, int z) {
    return x >= 0 && z >= 0 && x < width && z < height && land[z * width + x];
  }
}
