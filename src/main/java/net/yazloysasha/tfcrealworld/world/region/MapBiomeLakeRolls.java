package net.yazloysasha.tfcrealworld.world.region;

import java.util.function.IntUnaryOperator;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

/**
 * Map-driven lake rolls from 1.21.1 {@code ChooseBiomesMixin#rollMapLakes}.
 */
public final class MapBiomeLakeRolls {

  private static final double OCEANIC_MOUNTAIN_LAKE_CHANCE = 0.1;
  private static final float LAKE_RAINFALL_BOOST = 0.09f;
  private static final long OCEANIC_MOUNTAIN_LAKE_SALT = 0x3c9e2b71a4d805f1L;

  private MapBiomeLakeRolls() {}

  public static void rollOceanicMountainLakes(
    Region region,
    long worldSeed,
    int oceanicMountains,
    int volcanicOceanicMountains,
    IntUnaryOperator lakeFor
  ) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }

    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null || !point.land()) {
        continue;
      }
      if (point.lake()) {
        continue;
      }
      final int biome = point.biome;
      if (biome != oceanicMountains && biome != volcanicOceanicMountains) {
        continue;
      }
      final int gridX = RegionCoords.gridX(region, index);
      final int gridZ = RegionCoords.gridZ(region, index);
      if (
        !seededChance(
          worldSeed,
          gridX,
          gridZ,
          OCEANIC_MOUNTAIN_LAKE_SALT,
          OCEANIC_MOUNTAIN_LAKE_CHANCE
        )
      ) {
        continue;
      }
      point.setLake();
      point.rainfall += LAKE_RAINFALL_BOOST * (500f - point.rainfall);
      point.biome = lakeFor.applyAsInt(biome);
    }
  }

  private static boolean seededChance(
    long worldSeed,
    int gridX,
    int gridZ,
    long salt,
    double chance
  ) {
    long hash = worldSeed ^ salt;
    hash ^= (long) gridX * 0x9E3779B97F4A7C15L;
    hash ^= (long) gridZ * 0x6C078965L;
    hash = mix64(hash);
    return (hash >>> 11) * (1.0 / (1L << 53)) < chance;
  }

  private static long mix64(long z) {
    z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
    z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
    return z ^ (z >>> 33);
  }
}
