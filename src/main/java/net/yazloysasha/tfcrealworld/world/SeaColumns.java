package net.yazloysasha.tfcrealworld.world;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.dries007.tfc.world.biome.BiomeBlendType;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

/**
 * A column of the sea is no river. The generator names a column after the
 * heaviest land biome around it, so off a coast the sea is still that land,
 * which has rivers: a river that ends at the shore turns the sea before its
 * mouth into a river biome with flowing fresh water. Map rivers end in the
 * water, and there the sea stays the sea.
 */
public final class SeaColumns {

  /**
   * Share of ocean biomes around a column from which it is the sea: the
   * share from which the generator cuts the ground down to the sea's edge.
   */
  private static final double SEA_OCEAN_SHARE = 0.25;

  private SeaColumns() {}

  public static boolean isSea(Object2DoubleMap<BiomeExtension> biomeWeights) {
    if (
      !TFCRealWorldConfig.CONTINENT_FROM_MAP.get() ||
      !TFCRealWorldConfig.RIVERS_FROM_MAP.get()
    ) {
      return false;
    }
    double ocean = 0;
    for (final Object2DoubleMap.Entry<
      BiomeExtension
    > entry : biomeWeights.object2DoubleEntrySet()) {
      if (entry.getKey().biomeBlendType() == BiomeBlendType.OCEAN) {
        ocean += entry.getDoubleValue();
      }
    }
    return ocean >= SEA_OCEAN_SHARE;
  }
}
