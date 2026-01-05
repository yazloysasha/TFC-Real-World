package net.yazloysasha.tfcrealworld.util;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.noise.PNGAltitudeNoise;

/**
 * Registry for storing PNGAltitudeNoise instances for each RegionGenerator.
 */
public class AltitudeNoiseRegistry extends BaseNoiseRegistry<PNGAltitudeNoise> {

  private static final AltitudeNoiseRegistry INSTANCE =
    new AltitudeNoiseRegistry();

  private AltitudeNoiseRegistry() {}

  public static void register(
    RegionGenerator generator,
    PNGAltitudeNoise noise
  ) {
    INSTANCE.registerNoise(generator, noise);
  }

  public static PNGAltitudeNoise get(RegionGenerator generator) {
    return INSTANCE.getNoise(generator);
  }
}
