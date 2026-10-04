package net.yazloysasha.tfcrealworld.util.registry;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainVarianceNoise;
import org.jetbrains.annotations.Nullable;

/** TFC 3's generator has no rainfall variance noise to put the map in. */
public class RainVarianceRegistry
  extends BaseNoiseRegistry<PNGRainVarianceNoise>
{

  private static final RainVarianceRegistry INSTANCE =
    new RainVarianceRegistry();

  private RainVarianceRegistry() {}

  public static void register(
    RegionGenerator generator,
    PNGRainVarianceNoise noise
  ) {
    INSTANCE.registerNoise(generator, noise);
  }

  @Nullable
  public static PNGRainVarianceNoise get(RegionGenerator generator) {
    return TFCRealWorldConfig.CLIMATE_FROM_MAP.get()
      ? INSTANCE.getNoise(generator)
      : null;
  }
}
