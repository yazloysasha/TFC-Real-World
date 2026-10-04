package net.yazloysasha.tfcrealworld.world.volcano;

import net.dries007.tfc.world.noise.Noise2D;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * The intensity noises a generator shapes its shield volcanoes with, from
 * the hotspots of the tectonics map instead of its own hotspot chain.
 */
public final class MapHotspotIntensity {

  private static final byte OLDEST_AGE = 4;

  private MapHotspotIntensity() {}

  /** @return the noise, or null to leave the generator's own in place */
  @Nullable
  public static Noise2D ofAge(byte age, long seed) {
    final MapHotspotLayout layout = layout();
    return layout == null ? null : layout.intensityNoise(age, seed);
  }

  /** The strongest of the four ages, as the generator combines its own. */
  @Nullable
  public static Noise2D ofAnyAge(long seed) {
    final MapHotspotLayout layout = layout();
    if (layout == null) {
      return null;
    }
    final Noise2D[] ages = new Noise2D[OLDEST_AGE];
    for (byte age = 1; age <= OLDEST_AGE; age++) {
      ages[age - 1] = layout.intensityNoise(age, seed);
    }
    return (x, z) -> {
      double strongest = 0;
      for (final Noise2D age : ages) {
        strongest = Math.max(strongest, age.noise(x, z));
      }
      return strongest;
    };
  }

  @Nullable
  private static MapHotspotLayout layout() {
    return (
        TFCRealWorldConfig.CONTINENT_FROM_MAP.get() &&
        TFCRealWorldConfig.TECTONICS_FROM_MAP.get() &&
        TFCRealWorldConfig.VOLCANOES_FROM_MAP.get()
      )
      ? TectonicsRegistry.hotspotLayout()
      : null;
  }
}
