package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;

/**
 * Generates rainfall variance noise based on Köppen climate map.
 * Reads the Köppen climate from the map and generates procedural rainfall variance values
 * that are valid for that climate classification.
 *
 * Uses bilinear interpolation between neighboring climates for smooth transitions,
 * and adds procedural variations that are constrained to valid parameter ranges
 * for each climate type.
 *
 * Note: The returned value is for northern hemisphere. For southern hemisphere,
 * the sign should be inverted (as done in AnnotateClimateMixin).
 */
public class KoppenBasedRainfallVarianceNoise extends BaseKoppenBasedNoise {

  public KoppenBasedRainfallVarianceNoise(
    PNGKoppenNoise koppenNoise,
    long seed
  ) {
    super(koppenNoise, seed, 0.1f);
  }

  @Override
  protected double extractParameter(
    KoppenParameterCache.ParameterCombination params
  ) {
    return params.rainVar;
  }

  @Override
  protected double postProcessResult(double result) {
    return Math.clamp(result, -1.0, 1.0);
  }
}
