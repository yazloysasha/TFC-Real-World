package net.yazloysasha.tfcrealworld.world.layer;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;

/**
 * Context for re-applying the map land/ocean correction after vanilla shores.
 * Kept outside the mixin package so it is not treated as a mixin class.
 */
public final class PostShoreCorrection {

  public final PNGContinentNoise noise;
  public final RegionGenerator generator;

  public PostShoreCorrection(
    PNGContinentNoise noise,
    RegionGenerator generator
  ) {
    this.noise = noise;
    this.generator = generator;
  }
}
