package net.yazloysasha.tfcrealworld.util.registry;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.dries007.tfc.world.region.RegionGenerator;

public abstract class BaseNoiseRegistry<T> {

  protected final Map<RegionGenerator, T> registry =
    new Object2ObjectOpenHashMap<>();

  protected void registerNoise(RegionGenerator generator, T noise) {
    registry.put(generator, noise);
  }

  protected T getNoise(RegionGenerator generator) {
    return registry.get(generator);
  }
}
