package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;

/**
 * Generates temperature noise based on Köppen climate map.
 * Reads the Köppen climate from the map and generates procedural temperature values
 * that are valid for that climate classification.
 *
 * Uses bilinear interpolation between neighboring climates for smooth transitions,
 * and adds procedural variations that are constrained to valid parameter ranges
 * for each climate type.
 */
public class KoppenBasedTemperatureNoise extends BaseKoppenBasedNoise {

  public KoppenBasedTemperatureNoise(PNGKoppenNoise koppenNoise, long seed) {
    super(koppenNoise, seed, 0.1f);
  }

  @Override
  protected double extractParameter(
    KoppenParameterCache.ParameterCombination params
  ) {
    return params.temperature;
  }
}
