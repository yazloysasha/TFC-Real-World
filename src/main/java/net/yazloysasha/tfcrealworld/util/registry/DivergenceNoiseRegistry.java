package net.yazloysasha.tfcrealworld.util.registry;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGDivergenceNoise;

public class DivergenceNoiseRegistry
  extends BaseNoiseRegistry<PNGDivergenceNoise> {

  private static final DivergenceNoiseRegistry INSTANCE =
    new DivergenceNoiseRegistry();

  private DivergenceNoiseRegistry() {}

  public static void register(
    RegionGenerator generator,
    PNGDivergenceNoise noise
  ) {
    INSTANCE.registerNoise(generator, noise);
  }

  public static PNGDivergenceNoise get(RegionGenerator generator) {
    return INSTANCE.getNoise(generator);
  }
}
