package net.yazloysasha.tfcrealworld.world.noise;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;

/**
 * Generates temperature noise based on Köppen climate map.
 * Reads the Köppen climate from the map and generates procedural temperature values
 * that are valid for that climate classification.
 *
 * Uses bilinear interpolation between neighboring climates for smooth transitions,
 * and adds procedural variations that are constrained to valid parameter ranges
 * for each climate type.
 */
public class KoppenBasedTemperatureNoise implements Noise2D {

  private final PNGKoppenNoise koppenNoise;
  private final KoppenParameterCache parameterCache;
  private final Noise2D indexNoise; // Generates index (0.0-1.0) for parameter selection

  public KoppenBasedTemperatureNoise(PNGKoppenNoise koppenNoise, long seed) {
    this.koppenNoise = koppenNoise;
    this.parameterCache = KoppenParameterCache.getInstance();
    // Use the same seed for all three parameters to ensure consistency
    // Parameters match original TFC: spread 0.15f, 2 octaves for finer patterns
    this.indexNoise = new OpenSimplex2D(seed)
      .octaves(2)
      .spread(0.15f)
      .scaled(0.0, 1.0);
  }

  @Override
  public double noise(double x, double z) {
    PNGKoppenNoise.ClimateInterpolationResult interpolation =
      koppenNoise.getClimateInterpolation(x, z);

    double rawIndex = indexNoise.noise(x, z);
    double baseIndex = smoothstep(Math.clamp(rawIndex, 0.0, 1.0));

    // Generate indices for each corner with smooth variations
    // Use smaller variations for finer patterns (as in original TFC)
    double index00 = smoothstep(
      Math.clamp(
        baseIndex + (indexNoise.noise(x - 0.1, z - 0.1) - 0.5) * 0.08,
        0.0,
        1.0
      )
    );
    double index10 = smoothstep(
      Math.clamp(
        baseIndex + (indexNoise.noise(x + 0.1, z - 0.1) - 0.5) * 0.08,
        0.0,
        1.0
      )
    );
    double index01 = smoothstep(
      Math.clamp(
        baseIndex + (indexNoise.noise(x - 0.1, z + 0.1) - 0.5) * 0.08,
        0.0,
        1.0
      )
    );
    double index11 = smoothstep(
      Math.clamp(
        baseIndex + (indexNoise.noise(x + 0.1, z + 0.1) - 0.5) * 0.08,
        0.0,
        1.0
      )
    );

    // Get parameters from cache for each of the 4 climates
    // All parameters are guaranteed to belong to their zone
    KoppenParameterCache.ParameterCombination params00 =
      parameterCache.getParametersByIndex(interpolation.climate00, index00);
    KoppenParameterCache.ParameterCombination params10 =
      parameterCache.getParametersByIndex(interpolation.climate10, index10);
    KoppenParameterCache.ParameterCombination params01 =
      parameterCache.getParametersByIndex(interpolation.climate01, index01);
    KoppenParameterCache.ParameterCombination params11 =
      parameterCache.getParametersByIndex(interpolation.climate11, index11);

    double result =
      params00.temperature * interpolation.weight00 +
      params10.temperature * interpolation.weight10 +
      params01.temperature * interpolation.weight01 +
      params11.temperature * interpolation.weight11;

    return result;
  }

  /**
   * Smoothstep function for smoother interpolation.
   * Returns 0 for t=0, 1 for t=1, with smooth S-curve in between.
   */
  private double smoothstep(double t) {
    return t * t * (3.0 - 2.0 * t);
  }
}
