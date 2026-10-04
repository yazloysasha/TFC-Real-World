package net.yazloysasha.tfcrealworld.util.registry;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.dries007.tfc.world.region.RegionGenerator;
import org.jetbrains.annotations.Nullable;

/**
 * Per-generator noise. Weak keys: a generator dropped by the game (or a test)
 * releases its maps instead of keeping every map loaded since startup.
 */
public abstract class BaseNoiseRegistry<T> {

  private final Map<RegionGenerator, T> registry = Collections.synchronizedMap(
    new WeakHashMap<>()
  );

  private volatile @Nullable T latest;

  protected void registerNoise(RegionGenerator generator, T noise) {
    registry.put(generator, noise);
    latest = noise;
  }

  protected T getNoise(RegionGenerator generator) {
    return registry.get(generator);
  }

  /** The noise registered last (the world being generated now). */
  protected @Nullable T latestNoise() {
    return latest;
  }
}
