package net.yazloysasha.tfcrealworld.compat;

import net.dries007.tfc.world.ChunkGeneratorExtension;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.SpawnCenterHelper;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalOceanDistanceCache;
import su.terrafirmagreg.core.utils.CustomSpawnHelper;
import su.terrafirmagreg.core.utils.CustomSpawnHelper.CustomSpawnCondition;

/**
 * Core-Modern spawn presets on a Real World map. The {@code tfc_real_world}
 * preset uses {@link SpawnCenterHelper}; other overworld presets pick a search
 * center from map climate. Viewer and non-overworld presets are unchanged.
 */
public final class TfgSpawn {

  private static final int CLIMATE_GRID_SAMPLES = 16;
  private static final int HALF_GRID_BLOCK = Units.GRID_WIDTH_IN_BLOCK / 2;

  private static final ThreadLocal<Boolean> APPLYING_REMAP =
    ThreadLocal.withInitial(() -> Boolean.FALSE);

  private static final ThreadLocal<ClimateCache> CLIMATE_CENTER =
    new ThreadLocal<>();

  private TfgSpawn() {}

  public static CustomSpawnCondition createCondition() {
    CustomSpawnCondition defaults = CustomSpawnHelper.DEFAULT_SPAWN;
    return new CustomSpawnCondition(
      TFCRealWorld.MOD_ID,
      0,
      0,
      1,
      defaults.temperatureRange(),
      defaults.rainfallRange(),
      Level.OVERWORLD,
      Component.literal("")
    );
  }

  public static boolean isRealWorld(CustomSpawnCondition condition) {
    return condition != null && TFCRealWorld.MOD_ID.equals(condition.id());
  }

  public static BlockPos remapFindSpawnBiome(
    int spawnCenterX,
    int spawnCenterZ,
    int spawnRadius,
    RandomSource random,
    ChunkGeneratorExtension extension
  ) {
    if (APPLYING_REMAP.get()) {
      return null;
    }

    CustomSpawnCondition condition = CustomSpawnHelper.getFromConfig();
    int[] search = resolveSearch(condition, spawnRadius, extension, random);
    if (search == null) {
      return null;
    }

    int x = search[0];
    int z = search[1];
    int radius = search[2];
    if (x == spawnCenterX && z == spawnCenterZ && radius == spawnRadius) {
      return null;
    }

    APPLYING_REMAP.set(Boolean.TRUE);
    try {
      return CustomSpawnHelper.findSpawnBiome(x, z, radius, random, extension);
    } finally {
      APPLYING_REMAP.set(Boolean.FALSE);
    }
  }

  public static void clearClimateCenter() {
    CLIMATE_CENTER.remove();
  }

  private static int[] resolveSearch(
    CustomSpawnCondition condition,
    int spawnRadius,
    ChunkGeneratorExtension extension,
    RandomSource random
  ) {
    if (isRealWorld(condition)) {
      return new int[] {
        SpawnCenterHelper.getSpawnCenterX(),
        SpawnCenterHelper.getSpawnCenterZ(),
        SpawnCenterHelper.getSpawnDistance(),
      };
    }
    if (!usesMapClimate(condition)) {
      return null;
    }
    int[] center = climateCenter(condition, extension, random);
    return new int[] { center[0], center[1], spawnRadius };
  }

  private static boolean usesMapClimate(CustomSpawnCondition condition) {
    return (
      Level.OVERWORLD.equals(condition.dimension()) &&
      !CustomSpawnHelper.VIEWER_SPAWN_ID.equals(condition.id())
    );
  }

  private static int[] climateCenter(
    CustomSpawnCondition condition,
    ChunkGeneratorExtension extension,
    RandomSource random
  ) {
    ClimateCache cached = CLIMATE_CENTER.get();
    if (cached != null && cached.id().equals(condition.id())) {
      return cached.coords();
    }

    RegionGenerator generator = new RegionGenerator(
      extension.settings(),
      random
    );
    int extentX = Math.max(
      1,
      Units.blockToGrid(TFCRealWorldConfig.HORIZONTAL_SCALE.get())
    );
    int extentZ = Math.max(
      1,
      Units.blockToGrid(TFCRealWorldConfig.VERTICAL_SCALE.get())
    );
    int stepX = Math.max(1, extentX / CLIMATE_GRID_SAMPLES);
    int stepZ = Math.max(1, extentZ / CLIMATE_GRID_SAMPLES);

    int[] found = null;
    int matches = 0;
    for (int gx = -extentX; gx <= extentX; gx += stepX) {
      for (int gz = -extentZ; gz <= extentZ; gz += stepZ) {
        int sampleX = jitter(gx, stepX, extentX, random);
        int sampleZ = jitter(gz, stepZ, extentZ, random);
        if (!isLand(generator, sampleX, sampleZ)) {
          continue;
        }
        if (
          !CustomSpawnHelper.testWithinRanges(
            (float) generator.temperatureNoise.noise(sampleX, sampleZ),
            (float) generator.rainfallNoise.noise(sampleX, sampleZ),
            condition
          )
        ) {
          continue;
        }
        if (found == null || random.nextInt(matches + 1) == 0) {
          found = new int[] {
            gridToSpawnBlock(sampleX),
            gridToSpawnBlock(sampleZ),
          };
        }
        matches++;
      }
    }

    if (found == null) {
      found = new int[] {
        SpawnCenterHelper.getSpawnCenterX(),
        SpawnCenterHelper.getSpawnCenterZ(),
      };
    }
    CLIMATE_CENTER.set(new ClimateCache(condition.id(), found));
    return found;
  }

  private static int gridToSpawnBlock(int grid) {
    return RegionCoords.gridToBlock(grid) + HALF_GRID_BLOCK;
  }

  private record ClimateCache(String id, int[] coords) {}

  private static int jitter(
    int origin,
    int step,
    int extent,
    RandomSource random
  ) {
    if (step <= 1) {
      return origin;
    }
    int offset = random.nextInt(step) - step / 2;
    return Math.max(-extent, Math.min(extent, origin + offset));
  }

  private static boolean isLand(
    RegionGenerator generator,
    int gridX,
    int gridZ
  ) {
    GlobalOceanDistanceCache cache = GlobalOceanDistanceCache.getInstance();
    if (cache != null) {
      return cache.getDistance(gridX, gridZ, true) > 0;
    }
    return generator.continentNoise.noise(gridX, gridZ) > 0.0;
  }
}
