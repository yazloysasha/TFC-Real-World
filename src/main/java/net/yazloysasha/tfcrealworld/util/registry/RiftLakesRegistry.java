package net.yazloysasha.tfcrealworld.util.registry;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.tectonics.MapRiftLakes;
import org.jetbrains.annotations.Nullable;

public class RiftLakesRegistry extends BaseNoiseRegistry<MapRiftLakes> {

  private static final RiftLakesRegistry INSTANCE = new RiftLakesRegistry();

  private RiftLakesRegistry() {}

  public static void register(RegionGenerator generator, MapRiftLakes lakes) {
    INSTANCE.registerNoise(generator, lakes);
  }

  @Nullable
  public static MapRiftLakes get(RegionGenerator generator) {
    return TectonicsRegistry.isActive(generator)
      ? INSTANCE.getNoise(generator)
      : null;
  }
}
