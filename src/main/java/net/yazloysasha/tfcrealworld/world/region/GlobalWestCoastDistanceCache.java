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

  private GlobalWestCoastDistanceCache(
    int horizontalWorldScale,
    int verticalWorldScale
  ) {
    super(new PNGContinentNoise(horizontalWorldScale, verticalWorldScale));
    calculateDistances(continentNoise);

    LOGGER.info(
      "Global west coast distance cache initialized: {}x{}",
      width,
      height
    );
  }

  public static void initialize(
    int horizontalWorldScale,
    int verticalWorldScale
  ) {
    if (instance == null) {
      instance = new GlobalWestCoastDistanceCache(
        horizontalWorldScale,
        verticalWorldScale
      );
    }
  }

  @Nullable
  public static GlobalWestCoastDistanceCache getInstance() {
    return instance;
  }

  public byte getDistance(int gridX, int gridZ) {
    double clampedX = Math.clamp(gridX, -worldRadiusGridX, worldRadiusGridX);
    double clampedZ = Math.clamp(gridZ, -worldRadiusGridZ, worldRadiusGridZ);

    double imageX = centerX + clampedX * scaleX;
    double imageZ = centerZ + clampedZ * scaleZ;

    imageX = Math.clamp(imageX, 0, width - 1);
    imageZ = Math.clamp(imageZ, 0, height - 1);

    int x0 = (int) Math.floor(imageX);
    int z0 = (int) Math.floor(imageZ);
    int x1 = Math.min(x0 + 1, width - 1);
    int z1 = Math.min(z0 + 1, height - 1);

    double fx = imageX - x0;
    double fz = imageZ - z0;

    int idx00 = z0 * width + x0;
    int idx10 = z0 * width + x1;
    int idx01 = z1 * width + x0;
    int idx11 = z1 * width + x1;

    byte dist00 = distanceMap[idx00];
    byte dist10 = distanceMap[idx10];
    byte dist01 = distanceMap[idx01];
    byte dist11 = distanceMap[idx11];

    double dist0 = dist00 * (1 - fx) + dist10 * fx;
    double dist1 = dist01 * (1 - fx) + dist11 * fx;
    double finalDist = dist0 * (1 - fz) + dist1 * fz;
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

      final boolean canGoLeft = lastX > 0;
      final boolean canGoRight = lastX < width - 1;
      final boolean canGoUp = lastZ > 0;
      final boolean canGoDown = lastZ < height - 1;

      if (canGoLeft) {
        int idx = lastZ * width + (lastX - 1);
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.max(nextDistance, 0);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
      if (canGoRight) {
        int idx = lastZ * width + (lastX + 1);
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.max(nextDistance, 0);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
      if (canGoUp) {
        int idx = (lastZ - 1) * width + lastX;
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.max(nextDistance, 0);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
      if (canGoDown) {
        int idx = (lastZ + 1) * width + lastX;
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.max(nextDistance, 0);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
      if (canGoLeft && canGoUp) {
        int idx = (lastZ - 1) * width + (lastX - 1);
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.max(nextDistance, 0);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
      if (canGoRight && canGoUp) {
        int idx = (lastZ - 1) * width + (lastX + 1);
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.max(nextDistance, 0);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
      if (canGoLeft && canGoDown) {
        int idx = (lastZ + 1) * width + (lastX - 1);
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.max(nextDistance, 0);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
      if (canGoRight && canGoDown) {
        int idx = (lastZ + 1) * width + (lastX + 1);
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.max(nextDistance, 0);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
    }
  }
}
