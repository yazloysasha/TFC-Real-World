package net.yazloysasha.tfcrealworld.util;

public class WorldSeedHolder {

  private static final ThreadLocal<Long> SEED_HOLDER = new ThreadLocal<>();

  public static void setSeed(long seed) {
    SEED_HOLDER.set(seed);
  }

  public static long getSeed() {
    Long seed = SEED_HOLDER.get();
    return seed != null ? seed : 0L;
  }

  public static void clear() {
    SEED_HOLDER.remove();
  }
}
