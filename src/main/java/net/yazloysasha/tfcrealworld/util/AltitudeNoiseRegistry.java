package net.yazloysasha.tfcrealworld.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.noise.PNGAltitudeNoise;

/**
 * Registry for storing PNGAltitudeNoise instances for each RegionGenerator.
 */
public class AltitudeNoiseRegistry {

  private static final Map<RegionGenerator, PNGAltitudeNoise> REGISTRY =
    new Object2ObjectOpenHashMap<>();

  public static void register(
    RegionGenerator generator,
    PNGAltitudeNoise noise
  ) {
    REGISTRY.put(generator, noise);
  }

  public static PNGAltitudeNoise get(RegionGenerator generator) {
    return REGISTRY.get(generator);
  }

  public static void unregister(RegionGenerator generator) {
    REGISTRY.remove(generator);
  }
}
