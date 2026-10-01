package net.yazloysasha.tfcrealworld.util.registry;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import org.jetbrains.annotations.Nullable;

public class TectonicsRegistry extends BaseNoiseRegistry<TectonicsMap> {

  private static final TectonicsRegistry INSTANCE = new TectonicsRegistry();

  private static volatile @Nullable MapHotspotLayout hotspotLayout;

  private TectonicsRegistry() {}

  public static void register(
    RegionGenerator generator,
    TectonicsMap map,
    @Nullable MapHotspotLayout layout
  ) {
    INSTANCE.registerNoise(generator, map);
    hotspotLayout = layout;
  }

  @Nullable
  public static TectonicsMap get(RegionGenerator generator) {
    if (
      !TFCRealWorldConfig.CONTINENT_FROM_MAP.get() ||
      !TFCRealWorldConfig.TECTONICS_FROM_MAP.get()
    ) {
      return null;
    }
    return INSTANCE.getNoise(generator);
  }

  public static boolean isActive(RegionGenerator generator) {
    return get(generator) != null;
  }

  /**
   * Hotspot layout of the most recently created generator. Biome noises are
   * static in TFC, so they cannot look the layout up per generator.
   */
  @Nullable
  public static MapHotspotLayout hotspotLayout() {
    return hotspotLayout;
  }

  public static void clearHotspotLayout() {
    hotspotLayout = null;
  }
}
