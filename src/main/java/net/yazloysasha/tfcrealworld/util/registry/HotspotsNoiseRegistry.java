package net.yazloysasha.tfcrealworld.util.registry;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGHotspotsNoise;

/**
 * Registry for storing PNGHotspotsNoise instances for each RegionGenerator.
 */
public class HotspotsNoiseRegistry extends BaseNoiseRegistry<PNGHotspotsNoise> {

  private static final HotspotsNoiseRegistry INSTANCE =
    new HotspotsNoiseRegistry();

  private HotspotsNoiseRegistry() {}

  public static void register(
    RegionGenerator generator,
    PNGHotspotsNoise noise
  ) {
    INSTANCE.registerNoise(generator, noise);
  }

  public static PNGHotspotsNoise get(RegionGenerator generator) {
    return INSTANCE.getNoise(generator);
  }
}
