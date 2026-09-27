package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.DivergenceNoiseRegistry;

/**
 * Map plate-boundary helpers. Replaces vanilla cellular {@code divergence} and
 * {@code distanceToEdge} so ridges / rifts / trenches follow {@code divergence.png}
 * only — polarity matches vanilla: {@code < 0} subduction, {@code > 0} spreading.
 * Mountain / collision / volcanic biome selection stays vanilla after these
 * fields are set.
 */
public final class MapTectonics {

  private static final double MAX_RIFT_CONTINENT_ADJUST = -0.6;

  /** Far from any map boundary (vanilla cell interiors are ~this scale). */
  public static final byte INTERIOR_EDGE_DISTANCE = 24;

  private MapTectonics() {}

  public static boolean isActive(RegionGenerator generator) {
    return (
      TFCRealWorldConfig.CONTINENT_FROM_MAP.get() &&
      TFCRealWorldConfig.TECTONICS_FROM_MAP.get() &&
      DivergenceNoiseRegistry.get(generator) != null
    );
  }

  /**
   * Lower the altitude-derived continent continuum along divergent boundaries
   * (map analogue of {@code addRiftSeas}) so shelf / trench buckets shift.
   * Binary land from {@code continent.png} is unchanged.
   */
  public static double continentRiftAdjustment(float divergence) {
    if (divergence <= 0) {
      return 0;
    }
    float strength = Math.min(1f, divergence / 2f);
    return MAX_RIFT_CONTINENT_ADJUST * strength;
  }

  /**
   * Synthesize {@code distanceToEdge} from map |divergence| so vanilla
   * ChooseBiomes / AddMountains rift & collision checks follow the PNG instead
   * of Voronoi cells. Strong boundary → near 0; neutral →
   * {@link #INTERIOR_EDGE_DISTANCE}.
   */
  public static byte distanceToEdgeFromDivergence(float divergence) {
    float mag = Math.abs(divergence);
    if (mag <= 0f) {
      return INTERIOR_EDGE_DISTANCE;
    }
    float t = Math.min(1f, mag / 2f);
    return (byte) Math.round((1f - t) * 8f);
  }
}
