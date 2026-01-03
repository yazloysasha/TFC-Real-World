package net.yazloysasha.tfcrealworld.world.region;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.util.BitSet;
import net.yazloysasha.tfcrealworld.world.noise.PNGContinentNoise;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Global cache of distances to ocean based on continent map.
 * Calculates distance to ocean for the entire map once during initialization.
 */
public class GlobalOceanDistanceCache extends BaseDistanceCache {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Nullable
  private static GlobalOceanDistanceCache instance = null;

  private GlobalOceanDistanceCache(PNGContinentNoise continentNoise) {
    super(continentNoise);
    calculateDistances(continentNoise);

    LOGGER.info(
      "Global ocean distance cache initialized: {}x{}",
      width,
      height
    );
  }

  public static void initialize(PNGContinentNoise continentNoise) {
    if (instance == null) {
      instance = new GlobalOceanDistanceCache(continentNoise);
    }
  }

  public static void clear() {
    if (instance != null) {
      LOGGER.info("Clearing global ocean distance cache");
      instance = null;
    }
  }

  @Nullable
  public static GlobalOceanDistanceCache getInstance() {
    return instance;
  }

  public byte getDistance(int gridX, int gridZ, boolean isLand) {
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

    if (isLand) {
      double dist00Pos = dist00 > 0 ? dist00 : 0;
      double dist10Pos = dist10 > 0 ? dist10 : 0;
      double dist01Pos = dist01 > 0 ? dist01 : 0;
      double dist11Pos = dist11 > 0 ? dist11 : 0;

      double dist0 = dist00Pos * (1 - fx) + dist10Pos * fx;
      double dist1 = dist01Pos * (1 - fx) + dist11Pos * fx;
      double finalDist = dist0 * (1 - fz) + dist1 * fz;
      return (byte) Math.max(0, Math.round(finalDist));
    } else {
      if (dist00 == -2 || dist10 == -2 || dist01 == -2 || dist11 == -2) {
        return -2;
      }
      return (byte) Math.min(
        Math.min(dist00, dist10),
        Math.min(dist01, dist11)
      );
    }
  }

  private void calculateDistances(PNGContinentNoise continentNoise) {
    final BitSet explored = new BitSet(width * height);
    final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();

    for (int z = 0; z < height; z++) {
      int zWidth = z * width;
      for (int x = 0; x < width; x++) {
        int index = zWidth + x;
        boolean isOcean = isOceanPixel(x, z);
        if (isOcean) {
          distanceMap[index] = -1;
          queue.enqueue(index);
          explored.set(index);
        } else {
          distanceMap[index] = 0;
        }
      }
    }

    while (!queue.isEmpty()) {
      final int last = queue.dequeueInt();
      final int lastX = last % width;
      final int lastZ = last / width;
      final byte lastDist = distanceMap[last];
      final int nextDistance = lastDist + 1;

      final boolean canGoLeft = lastX > 0;
      final boolean canGoRight = lastX < width - 1;
      final boolean canGoUp = lastZ > 0;
      final boolean canGoDown = lastZ < height - 1;

      if (canGoLeft) {
        int idx = lastZ * width + (lastX - 1);
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.min(nextDistance, 127);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
      if (canGoRight) {
        int idx = lastZ * width + (lastX + 1);
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.min(nextDistance, 127);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
      if (canGoUp) {
        int idx = (lastZ - 1) * width + lastX;
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.min(nextDistance, 127);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
      if (canGoDown) {
        int idx = (lastZ + 1) * width + lastX;
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.min(nextDistance, 127);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
      if (canGoLeft && canGoUp) {
        int idx = (lastZ - 1) * width + (lastX - 1);
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.min(nextDistance, 127);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
      if (canGoRight && canGoUp) {
        int idx = (lastZ - 1) * width + (lastX + 1);
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.min(nextDistance, 127);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
      if (canGoLeft && canGoDown) {
        int idx = (lastZ + 1) * width + (lastX - 1);
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.min(nextDistance, 127);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
      if (canGoRight && canGoDown) {
        int idx = (lastZ + 1) * width + (lastX + 1);
        if (distanceMap[idx] == 0 && !explored.get(idx)) {
          distanceMap[idx] = (byte) Math.min(nextDistance, 127);
          queue.enqueue(idx);
          explored.set(idx);
        }
      }
    }

    for (int z = 0; z < height; z++) {
      int zWidth = z * width;
      boolean zValid = z > 0 && z < height - 1;
      for (int x = 0; x < width; x++) {
        int index = zWidth + x;
        if (distanceMap[index] == -1) {
          boolean hasLandNeighbor = false;
          boolean xValid = x > 0 && x < width - 1;

          if (xValid && zValid) {
            int idxLeft = zWidth + (x - 1);
            int idxRight = zWidth + (x + 1);
            int idxUp = (z - 1) * width + x;
            int idxDown = (z + 1) * width + x;
            int idxUpLeft = (z - 1) * width + (x - 1);
            int idxUpRight = (z - 1) * width + (x + 1);
            int idxDownLeft = (z + 1) * width + (x - 1);
            int idxDownRight = (z + 1) * width + (x + 1);

            if (
              distanceMap[idxLeft] > 0 ||
              distanceMap[idxRight] > 0 ||
              distanceMap[idxUp] > 0 ||
              distanceMap[idxDown] > 0 ||
              distanceMap[idxUpLeft] > 0 ||
              distanceMap[idxUpRight] > 0 ||
              distanceMap[idxDownLeft] > 0 ||
              distanceMap[idxDownRight] > 0
            ) {
              hasLandNeighbor = true;
            }
          } else {
            for (int dx = -1; dx <= 1; dx++) {
              for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                int nx = x + dx;
                int nz = z + dz;
                if (
                  nx >= 0 &&
                  nx < width &&
                  nz >= 0 &&
                  nz < height &&
                  distanceMap[nz * width + nx] > 0
                ) {
                  hasLandNeighbor = true;
                  break;
                }
              }
              if (hasLandNeighbor) break;
            }
          }

          if (hasLandNeighbor) {
            distanceMap[index] = -2;
          }
        }
      }
    }
  }
}
