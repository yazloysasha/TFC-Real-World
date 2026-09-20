package net.yazloysasha.tfcrealworld.world.volcano;

import net.dries007.tfc.world.noise.Noise2D;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;

public final class MapHotspotNoise {

  private MapHotspotNoise() {}

  public static Noise2D forAge(byte age, long seed) {
    return (x, z) -> {
      if (!TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
        return 0;
      }
      final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
      if (layout == null) {
        return 0;
      }
      return layout.sampleIntensityForAge(age, seed, x, z);
    };
  }

  public static Noise2D combined(long seed) {
    return (x, z) -> {
      if (!TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
        return 0;
      }
      final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
      if (layout == null) {
        return 0;
      }
      return layout.combinedIntensity(x, z, seed);
    };
  }
}
