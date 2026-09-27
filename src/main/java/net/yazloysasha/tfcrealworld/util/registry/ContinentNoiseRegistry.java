package net.yazloysasha.tfcrealworld.util.registry;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import org.jetbrains.annotations.Nullable;

/**
 * Holds per-{@link RegionGenerator} continent noise for biome-layer sampling.
 */
public class ContinentNoiseRegistry
  extends BaseNoiseRegistry<PNGContinentNoise> {

  private static final ContinentNoiseRegistry INSTANCE =
    new ContinentNoiseRegistry();

  private ContinentNoiseRegistry() {}

  public static void register(
    RegionGenerator generator,
    PNGContinentNoise noise
  ) {
    INSTANCE.registerNoise(generator, noise);
  }

  @Nullable
  public static PNGContinentNoise get(RegionGenerator generator) {
    return INSTANCE.getNoise(generator);
  }
}
