package net.yazloysasha.tfcrealworld.util.helpers;

import java.util.Random;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.types.CachedSpawnCenter;
import net.yazloysasha.tfcrealworld.types.SpawnMode;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;

public class SpawnCenterHelper {

  private static final ThreadLocal<CachedSpawnCenter> SPAWN_CENTER_CACHE =
    new ThreadLocal<>();

  public static int getSpawnDistance() {
    SpawnMode mode = TFCRealWorldConfig.SPAWN_MODE.get();
    int value = TFCRealWorldConfig.SPAWN_DISTANCE.get();

    if (mode == SpawnMode.RANDOM) {
      int halfX = TFCRealWorldConfig.HORIZONTAL_SCALE.get();
      int halfZ = TFCRealWorldConfig.VERTICAL_SCALE.get();
      value = Math.min(halfX, halfZ);
    }

    return value;
  }

  public static int getSpawnCenterX() {
    int[] spawnCenter = getSpawnCenter();
    return spawnCenter[0];
  }

  public static int getSpawnCenterZ() {
    int[] spawnCenter = getSpawnCenter();
    return spawnCenter[1];
  }

  private static int[] generateRandomSpawnCenter(long seed) {
    Random rng = new Random(seed ^ 0x1234ABCDL);
    int halfX = TFCRealWorldConfig.HORIZONTAL_SCALE.get();
    int halfZ = TFCRealWorldConfig.VERTICAL_SCALE.get();

    // For RANDOM mode, SPAWN_DISTANCE is a square movement radius
    int spawnDistance = Math.min(halfX, halfZ);

    // Ensure SPAWN_CENTER ± SPAWN_DISTANCE stays within the scale radius:
    // |center| + spawnDistance <= scale on each axis
    int maxCenterX = Math.max(0, halfX - spawnDistance);
    int maxCenterZ = Math.max(0, halfZ - spawnDistance);

    int x = maxCenterX == 0 ? 0 : rng.nextInt(maxCenterX * 2 + 1) - maxCenterX;
    int z = maxCenterZ == 0 ? 0 : rng.nextInt(maxCenterZ * 2 + 1) - maxCenterZ;

    return new int[] { x, z };
  }

  private static int[] getSpawnCenter() {
    SpawnMode mode = TFCRealWorldConfig.SPAWN_MODE.get();

    long seed = mode == SpawnMode.RANDOM ? WorldSeedHolder.getSeed() : 0L;
    CachedSpawnCenter cachedSpawnCenter = SPAWN_CENTER_CACHE.get();
    if (
      cachedSpawnCenter != null &&
      cachedSpawnCenter.mode() == mode &&
      (mode != SpawnMode.RANDOM || cachedSpawnCenter.seed() == seed)
    ) {
      return cachedSpawnCenter.coords();
    }

    int[] result =
      switch (mode) {
        case GEOGRAPHIC -> {
          double[] geoCoords = ProjectionManager.geographicToClassic(
            TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.get(),
            TFCRealWorldConfig.SPAWN_CENTER_LATITUDE.get()
          );
          yield new int[] {
            (int) Math.round(geoCoords[0]),
            (int) Math.round(geoCoords[1]),
          };
        }
        case CLASSIC -> new int[] {
          TFCRealWorldConfig.SPAWN_CENTER_X.get(),
          TFCRealWorldConfig.SPAWN_CENTER_Z.get(),
        };
        case RANDOM -> generateRandomSpawnCenter(seed);
      };

    SPAWN_CENTER_CACHE.set(new CachedSpawnCenter(mode, seed, result));

    return result;
  }
}
