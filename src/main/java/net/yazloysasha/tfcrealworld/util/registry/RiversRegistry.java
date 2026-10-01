package net.yazloysasha.tfcrealworld.util.registry;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.river.MapRivers;
import org.jetbrains.annotations.Nullable;

public class RiversRegistry extends BaseNoiseRegistry<MapRivers> {

  private static final RiversRegistry INSTANCE = new RiversRegistry();

  private RiversRegistry() {}

  public static void register(RegionGenerator generator, MapRivers rivers) {
    INSTANCE.registerNoise(generator, rivers);
  }

  @Nullable
  public static MapRivers get(RegionGenerator generator) {
    return (
        TFCRealWorldConfig.CONTINENT_FROM_MAP.get() &&
        TFCRealWorldConfig.RIVERS_FROM_MAP.get()
      )
      ? INSTANCE.getNoise(generator)
      : null;
  }
}
