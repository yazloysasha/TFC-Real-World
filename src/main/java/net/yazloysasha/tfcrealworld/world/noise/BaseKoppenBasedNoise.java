package net.yazloysasha.tfcrealworld.world.noise;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;

/**
 * Base class for Köppen-based noise generators.
 * Provides common functionality for generating noise values based on Köppen climate maps.
 * Subclasses specify which parameter (temperature, rainfall, or rainVar) to extract.
 */
public abstract class BaseKoppenBasedNoise implements Noise2D {

  protected final PNGKoppenNoise koppenNoise;
  protected final KoppenParameterCache parameterCache;
  protected final Noise2D indexNoise; // Generates index (0.0-1.0) for parameter selection

  protected BaseKoppenBasedNoise(
    PNGKoppenNoise koppenNoise,
    long seed,
    float spread
  ) {
    this.koppenNoise = koppenNoise;
    this.parameterCache = KoppenParameterCache.getInstance();
    // Use the same seed for all three parameters to ensure consistency
    // Parameters match original TFC: 2 octaves for finer patterns
    this.indexNoise = new OpenSimplex2D(seed)
      .octaves(2)
      .spread(spread)
      .scaled(0.0, 1.0);
  }

  @Override
  public double noise(double x, double z) {
    PNGKoppenNoise.ClimateInterpolationResult interpolation =
      koppenNoise.getClimateInterpolation(x, z);

    double[] indices = calculateIndices(x, z);

    // Get parameters from cache for each of the 4 climates
    // All parameters are guaranteed to belong to their zone
    KoppenParameterCache.ParameterCombination params00 =
      parameterCache.getParametersByIndex(interpolation.climate00, indices[0]);
    KoppenParameterCache.ParameterCombination params10 =
      parameterCache.getParametersByIndex(interpolation.climate10, indices[1]);
    KoppenParameterCache.ParameterCombination params01 =
      parameterCache.getParametersByIndex(interpolation.climate01, indices[2]);
    KoppenParameterCache.ParameterCombination params11 =
      parameterCache.getParametersByIndex(interpolation.climate11, indices[3]);

    double result =
      extractParameter(params00) * interpolation.weight00 +
      extractParameter(params10) * interpolation.weight10 +
      extractParameter(params01) * interpolation.weight01 +
      extractParameter(params11) * interpolation.weight11;

    return postProcessResult(result);
  }

  /**
   * Calculates indices for each corner with smooth variations.
   * Returns array of 4 indices: [index00, index10, index01, index11]
   */
  protected double[] calculateIndices(double x, double z) {
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

    return new double[] { index00, index10, index01, index11 };
  }

  /**
   * Smoothstep function for smoother interpolation.
   * Returns 0 for t=0, 1 for t=1, with smooth S-curve in between.
   */
  protected double smoothstep(double t) {
    return t * t * (3.0 - 2.0 * t);
  }

  /**
   * Extracts the relevant parameter from a ParameterCombination.
   * Subclasses override this to specify which parameter to use.
   */
  protected abstract double extractParameter(
    KoppenParameterCache.ParameterCombination params
  );

  /**
   * Post-processes the result value.
   * Subclasses can override this to apply clamping or other transformations.
   * Default implementation returns the value as-is.
   */
  protected double postProcessResult(double result) {
    return result;
  }
}
