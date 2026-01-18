package net.yazloysasha.tfcrealworld.util.registry;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.dries007.tfc.world.region.RegionGenerator;

public abstract class BaseNoiseRegistry<T> {

  protected final Map<RegionGenerator, T> registry =
    Collections.synchronizedMap(new WeakHashMap<>());

  protected void registerNoise(RegionGenerator generator, T noise) {
    registry.put(generator, noise);
  }

  protected T getNoise(RegionGenerator generator) {
    return registry.get(generator);
  }
}
