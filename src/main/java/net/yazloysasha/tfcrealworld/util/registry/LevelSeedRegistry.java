package net.yazloysasha.tfcrealworld.util.registry;

import net.dries007.tfc.world.region.RegionGenerator;

/**
 * The level seed of each generator. TFC 3's generator only keeps a seed
 * derived from it, while biome noise is seeded with the level seed itself.
 */
public class LevelSeedRegistry extends BaseNoiseRegistry<Long> {

  private static final LevelSeedRegistry INSTANCE = new LevelSeedRegistry();

  private LevelSeedRegistry() {}

  public static void register(RegionGenerator generator, long seed) {
    INSTANCE.registerNoise(generator, seed);
  }

  /** The seed of the world being generated now, for static biome noise. */
  public static long latest() {
    final Long seed = INSTANCE.latestNoise();
    return seed != null ? seed : 0L;
  }

  public static long get(RegionGenerator generator) {
    final Long seed = INSTANCE.getNoise(generator);
    return seed != null ? seed : generator.seed();
  }
}
