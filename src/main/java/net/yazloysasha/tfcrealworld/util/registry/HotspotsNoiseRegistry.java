package net.yazloysasha.tfcrealworld.util.registry;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGHotspotsNoise;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import org.jetbrains.annotations.Nullable;

/**
 * Registry for storing PNGHotspotsNoise instances for each RegionGenerator.
 */
public class HotspotsNoiseRegistry extends BaseNoiseRegistry<PNGHotspotsNoise> {

  private static final HotspotsNoiseRegistry INSTANCE =
    new HotspotsNoiseRegistry();

  private static volatile @Nullable MapHotspotLayout biomeLayout;

  private HotspotsNoiseRegistry() {}

  public static void register(
    RegionGenerator generator,
    PNGHotspotsNoise noise
  ) {
    INSTANCE.registerNoise(generator, noise);
    biomeLayout = noise.layout();
  }

  public static PNGHotspotsNoise get(RegionGenerator generator) {
    return INSTANCE.getNoise(generator);
  }

  @Nullable
  public static MapHotspotLayout biomeLayout() {
    return biomeLayout;
  }

  public static void clearBiomeLayout() {
    biomeLayout = null;
  }
}
