package net.yazloysasha.tfcrealworld.world.noise;

/**
 * Generates rainfall noise based on Köppen climate map.
 * Reads the Köppen climate from the map and generates procedural rainfall values
 * that are valid for that climate classification.
 *
 * Uses bilinear interpolation between neighboring climates for smooth transitions,
 * and adds procedural variations that are constrained to valid parameter ranges
 * for each climate type.
 */
public class KoppenBasedRainfallNoise extends BaseKoppenBasedNoise {

  public KoppenBasedRainfallNoise(PNGKoppenNoise koppenNoise, long seed) {
    super(koppenNoise, seed, 0.1f);
  }

  @Override
  protected double extractParameter(
    KoppenParameterCache.ParameterCombination params
  ) {
    return params.rainfall;
  }

  @Override
  protected double postProcessResult(double result) {
    return Math.clamp(result, 0.0, 500.0);
  }
}
