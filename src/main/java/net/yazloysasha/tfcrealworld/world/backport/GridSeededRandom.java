package net.yazloysasha.tfcrealworld.world.backport;

/**
 * Deterministic per-grid rolls shared by TFC 4 / TFG / TFE choose-biomes tails.
 */
public final class GridSeededRandom {

  private GridSeededRandom() {}

  public static boolean chance(
    long worldSeed,
    int gridX,
    int gridZ,
    long salt,
    double probability
  ) {
    return unit(worldSeed, gridX, gridZ, salt) < probability;
  }

  public static float signedUnit(
    long worldSeed,
    int gridX,
    int gridZ,
    long salt
  ) {
    return (float) (unit(worldSeed, gridX, gridZ, salt) * 2.0 - 1.0);
  }

  public static double unit(long worldSeed, int gridX, int gridZ, long salt) {
    long hash = worldSeed ^ salt;
    hash ^= (long) gridX * 0x9E3779B97F4A7C15L;
    hash ^= (long) gridZ * 0x6C078965L;
    hash = mix64(hash);
    return (hash >>> 11) * (1.0 / (1L << 53));
  }

  public static long mix64(long z) {
    z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
    z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
    return z ^ (z >>> 33);
  }
}
