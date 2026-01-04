package net.yazloysasha.tfcrealworld.world.region;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.util.BitSet;
import net.yazloysasha.tfcrealworld.world.noise.PNGContinentNoise;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Global cache of distances to west coast based on continent map.
 * Calculates distance to west coast for the entire map once during initialization.
 */
public class GlobalWestCoastDistanceCache extends BaseDistanceCache {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Nullable
  private static GlobalWestCoastDistanceCache instance = null;

  private GlobalWestCoastDistanceCache(PNGContinentNoise continentNoise) {
    super(continentNoise);
    calculateDistances(continentNoise);

    LOGGER.info(
      "Global west coast distance cache initialized: {}x{}",
      width,
      height
    );
  }

  public static void initialize(PNGContinentNoise continentNoise) {
    if (instance == null) {
      instance = new GlobalWestCoastDistanceCache(continentNoise);
    }
  }

  public static void clear() {
    if (instance != null) {
      LOGGER.info("Clearing global west coast distance cache");
      instance = null;
    }
  }

  @Nullable
  public static GlobalWestCoastDistanceCache getInstance() {
    return instance;
  }

  public byte getDistance(int gridX, int gridZ) {
    InterpolationResult interpolation = getInterpolationData(gridX, gridZ);

    int idx00 = interpolation.z0 * width + interpolation.x0;
    int idx10 = interpolation.z0 * width + interpolation.x1;
    int idx01 = interpolation.z1 * width + interpolation.x0;
    int idx11 = interpolation.z1 * width + interpolation.x1;

    byte dist00 = distanceMap[idx00];
    byte dist10 = distanceMap[idx10];
    byte dist01 = distanceMap[idx01];
    byte dist11 = distanceMap[idx11];

    double dist0 = dist00 * (1 - interpolation.fx) + dist10 * interpolation.fx;
    double dist1 = dist01 * (1 - interpolation.fx) + dist11 * interpolation.fx;
    double finalDist =
      dist0 * (1 - interpolation.fz) + dist1 * interpolation.fz;
    return (byte) Math.max(0, Math.round(finalDist));
  }

  private void calculateDistances(PNGContinentNoise continentNoise) {
    final boolean[] isLandMap = new boolean[width * height];
    for (int z = 0; z < height; z++) {
      for (int x = 0; x < width; x++) {
        isLandMap[z * width + x] = !isOceanPixel(x, z);
      }
    }

    for (int x = 0; x < width; x++) {
      for (int z = 0; z < height; z++) {
        int index = z * width + x;
        boolean isLand = isLandMap[index];

        if (x == 0) {
          distanceMap[index] = 0;
        } else {
          int prevIndex = z * width + (x - 1);
          byte prevValue = distanceMap[prevIndex];

          if (!isLand) {
            distanceMap[index] = (byte) Math.max(prevValue - 2, 0);
          } else {
            int sum = prevValue;
            int count = 1;

            for (int dz = -2; dz <= 2; dz++) {
              int nz = z + dz;
              if (nz >= 0 && nz < height && dz != 0) {
                int neighborIndex = nz * width + (x - 1);
                sum += distanceMap[neighborIndex];
                count++;
              }
            }

            distanceMap[index] = (byte) Math.min(
              (int) Math.ceil(sum / (double) count) + 1,
              127
            );
          }
        }
      }
    }

    final BitSet explored = new BitSet(width * height);
    final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();

    for (int z = 0; z < height; z++) {
      int zWidth = z * width;
      for (int x = 0; x < width; x++) {
        int index = zWidth + x;
        if (isLandMap[index] && distanceMap[index] > 0) {
          explored.set(index);
          queue.enqueue(index);
        }
      }
    }

    while (!queue.isEmpty()) {
      final int last = queue.dequeueInt();
      final int lastX = last % width;
      final int lastZ = last / width;
      final byte lastDist = distanceMap[last];
      final int nextDistance = lastDist + (lastDist > 40 ? -1 : 1);

      processNeighbors(lastX, lastZ, nextDistance, explored, queue, d ->
        Math.max(d, 0)
      );
    }
  }
}
