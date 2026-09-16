package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.DivergenceNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGDivergenceNoise;

public final class MapTectonics {

  public static final float RIFT_BIOME_THRESHOLD = 1.0f;
  public static final int LAND_RIFT_CORE_RADIUS = 1;
  public static final float LAND_RIFT_BELT_MIN = 0.05f;
  public static final float OCEAN_RIDGE_DIVERGENCE = 1.0f;
  public static final float TRENCH_DIVERGENCE = -0.6f;

  private static final double MAX_RIFT_CONTINENT_ADJUST = -0.6;

  private MapTectonics() {}

  public static boolean isActive(RegionGenerator generator) {
    return (
      TFCRealWorldConfig.CONTINENT_FROM_MAP.get() &&
      TFCRealWorldConfig.TECTONICS_FROM_MAP.get() &&
      DivergenceNoiseRegistry.get(generator) != null
    );
  }

  public static double continentRiftAdjustment(float divergence) {
    if (divergence <= 0) {
      return 0;
    }
    float strength = Math.min(1f, divergence / 2f);
    return MAX_RIFT_CONTINENT_ADJUST * strength;
  }

  public static boolean isLandRiftCore(PNGDivergenceNoise noise, int x, int z) {
    if (noise.getDivergence(x, z) <= RIFT_BIOME_THRESHOLD) {
      return false;
    }
    return allBeyond(noise, x, z, LAND_RIFT_CORE_RADIUS, LAND_RIFT_BELT_MIN);
  }

  public static boolean isNearOceanRidge(
    PNGDivergenceNoise noise,
    int x,
    int z
  ) {
    return noise.getDivergence(x, z) > OCEAN_RIDGE_DIVERGENCE;
  }

  public static boolean isNearTrench(PNGDivergenceNoise noise, int x, int z) {
    return noise.getDivergence(x, z) < TRENCH_DIVERGENCE;
  }

  private static boolean allBeyond(
    PNGDivergenceNoise noise,
    int x,
    int z,
    int radius,
    float min
  ) {
    for (int dz = -radius; dz <= radius; dz++) {
      for (int dx = -radius; dx <= radius; dx++) {
        if (noise.getDivergence(x + dx, z + dz) <= min) {
          return false;
        }
      }
    }
    return true;
  }
}
