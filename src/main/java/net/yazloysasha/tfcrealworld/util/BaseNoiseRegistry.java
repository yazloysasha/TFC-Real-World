package net.yazloysasha.tfcrealworld.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.dries007.tfc.world.region.RegionGenerator;

/**
 * Base class for noise registries that store noise instances per RegionGenerator.
 * Provides common registry field and structure.
 */
public abstract class BaseNoiseRegistry<T> {

  protected final Map<RegionGenerator, T> registry =
    new Object2ObjectOpenHashMap<>();

  protected void registerNoise(RegionGenerator generator, T noise) {
    registry.put(generator, noise);
  }

  protected T getNoise(RegionGenerator generator) {
    return registry.get(generator);
  }

  protected void unregisterNoise(RegionGenerator generator) {
    registry.remove(generator);
  }
}
