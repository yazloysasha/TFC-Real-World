package net.yazloysasha.tfcrealworld.world.region.cache;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.util.BitSet;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import org.jetbrains.annotations.Nullable;

/**
 * Vanilla's AnnotateDistanceToWestCoast over the whole map: land gains one
 * for every cell east of its west coast, the sea east of a shore loses two,
 * and the rest of the sea takes its value from the nearest coast.
 */
public class GlobalWestCoastDistanceCache extends BaseDistanceCache {

  /** Vanilla: the sea counts up from the coast to this, then down. */
  private static final int SEA_TURN = 40;

  @Nullable
  private static volatile GlobalWestCoastDistanceCache instance = null;

  private GlobalWestCoastDistanceCache(PNGContinentNoise continent) {
    super(continent);
    for (int x = 1; x < width; x++) {
      for (int z = 0; z < height; z++) {
        final int west = distances[z * width + x - 1];
        if (!land[z * width + x]) {
          distances[z * width + x] = (byte) Math.max(west - 2, 0);
          continue;
        }
        int sum = 0;
        for (int dz = -2; dz <= 2; dz++) {
          final int nz = z + dz;
          sum += nz >= 0 && nz < height ? distances[nz * width + x - 1] : west;
        }
        distances[z * width + x] = (byte) Math.min(
          (int) Math.ceil(sum / 5.0) + 1,
          Byte.MAX_VALUE
        );
      }
    }

    final BitSet explored = new BitSet(width * height);
    final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
    for (int cell = 0; cell < land.length; cell++) {
      if (land[cell]) {
        explored.set(cell);
        queue.enqueue(cell);
      }
    }
    while (!queue.isEmpty()) {
      final int last = queue.dequeueInt();
      final int next = distances[last] + (distances[last] > SEA_TURN ? -1 : 1);
      for (int dz = -1; dz <= 1; dz++) {
        for (int dx = -1; dx <= 1; dx++) {
          final int x = (last % width) + dx;
          final int z = last / width + dz;
          if (x < 0 || z < 0 || x >= width || z >= height) {
            continue;
          }
          final int cell = z * width + x;
          if (distances[cell] == 0 && !explored.get(cell)) {
            distances[cell] = (byte) Math.max(next, 0);
            queue.enqueue(cell);
          }
          explored.set(cell);
        }
      }
    }
  }

  public static void initialize(PNGContinentNoise continent) {
    if (instance == null || !instance.isBuiltFrom(continent)) {
      instance = new GlobalWestCoastDistanceCache(continent);
    }
  }

  public static void clear() {
    instance = null;
  }

  @Nullable
  public static GlobalWestCoastDistanceCache getInstance() {
    return instance;
  }

  public byte getDistance(int gridX, int gridZ) {
    return distanceAt(gridX, gridZ);
  }
}
