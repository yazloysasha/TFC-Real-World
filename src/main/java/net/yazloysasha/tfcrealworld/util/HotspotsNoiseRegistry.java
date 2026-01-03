package net.yazloysasha.tfcrealworld.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.noise.PNGHotspotsNoise;

public class HotspotsNoiseRegistry {

  private static final Map<RegionGenerator, PNGHotspotsNoise> REGISTRY =
    new Object2ObjectOpenHashMap<>();

  public static void register(
    RegionGenerator generator,
    PNGHotspotsNoise hotspotsNoise
  ) {
    REGISTRY.put(generator, hotspotsNoise);
  }

  public static PNGHotspotsNoise get(RegionGenerator generator) {
    return REGISTRY.get(generator);
  }

  public static void unregister(RegionGenerator generator) {
    REGISTRY.remove(generator);
  }
}
