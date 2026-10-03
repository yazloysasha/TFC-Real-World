package net.yazloysasha.tfcrealworld.world.region.cache;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import org.jetbrains.annotations.Nullable;

/**
 * Vanilla's AnnotateDistanceToOcean over the whole map: land next to the sea
 * is 0 and every cell further inland one more. The sea is -1, and -2 where
 * it lies next to land.
 */
public class GlobalOceanDistanceCache extends BaseDistanceCache {

  private static final byte SEA = -1;
  private static final byte SEA_BY_LAND = -2;

  @Nullable
  private static volatile GlobalOceanDistanceCache instance = null;

  private GlobalOceanDistanceCache(PNGContinentNoise continent) {
    super(continent);
    final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
    for (int z = 0; z < height; z++) {
      for (int x = 0; x < width; x++) {
        final int cell = z * width + x;
        if (land[cell]) {
          // Not reached yet; no land is this far from the sea.
          distances[cell] = Byte.MAX_VALUE;
        } else {
          distances[cell] = hasLandNeighbour(x, z) ? SEA_BY_LAND : SEA;
          if (distances[cell] == SEA_BY_LAND) {
            queue.enqueue(cell);
          }
        }
      }
    }
    while (!queue.isEmpty()) {
      final int last = queue.dequeueInt();
      final int next = Math.min(
        Math.max(distances[last], SEA) + 1,
        Byte.MAX_VALUE - 1
      );
      for (int dz = -1; dz <= 1; dz++) {
        for (int dx = -1; dx <= 1; dx++) {
          final int x = (last % width) + dx;
          final int z = last / width + dz;
          if (isLand(x, z) && distances[z * width + x] == Byte.MAX_VALUE) {
            distances[z * width + x] = (byte) next;
            queue.enqueue(z * width + x);
          }
        }
      }
    }
  }

  public static void initialize(PNGContinentNoise continent) {
    if (instance == null || !instance.isBuiltFrom(continent)) {
      instance = new GlobalOceanDistanceCache(continent);
    }
  }

  public static void clear() {
    instance = null;
  }

  @Nullable
  public static GlobalOceanDistanceCache getInstance() {
    return instance;
  }

  /**
   * @param isLand whether the region has land at the point: an islet the
   *     map's cell centre misses is land by the sea.
   */
  public byte getDistance(int gridX, int gridZ, boolean isLand) {
    final byte distance = distanceAt(gridX, gridZ);
    return isLand ? (byte) Math.max(distance, 0) : distance;
  }

  private boolean hasLandNeighbour(int x, int z) {
    for (int dz = -1; dz <= 1; dz++) {
      for (int dx = -1; dx <= 1; dx++) {
        if (isLand(x + dx, z + dz)) {
          return true;
        }
      }
    }
    return false;
  }
}
