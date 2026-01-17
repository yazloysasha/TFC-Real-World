package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;

/**
 * Base class for Köppen-based noise generators.
 */
public abstract class BaseKoppenBasedNoise implements Noise2D {

  protected final PNGKoppenNoise koppenNoise;
  protected final KoppenParameterCache parameterCache;
  protected final Noise2D indexNoise;

  protected BaseKoppenBasedNoise(
    PNGKoppenNoise koppenNoise,
    long seed,
    float spread
  ) {
    this.koppenNoise = koppenNoise;
    this.parameterCache = KoppenParameterCache.getInstance();
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

  protected double[] calculateIndices(double x, double z) {
    double rawIndex = indexNoise.noise(x, z);
    double baseIndex = smoothstep(Mth.clamp(rawIndex, 0.0, 1.0));

    return new double[] {
      calculateCornerIndex(x - 0.1, z - 0.1, baseIndex),
      calculateCornerIndex(x + 0.1, z - 0.1, baseIndex),
      calculateCornerIndex(x - 0.1, z + 0.1, baseIndex),
      calculateCornerIndex(x + 0.1, z + 0.1, baseIndex),
    };
  }

  private double calculateCornerIndex(double x, double z, double baseIndex) {
    return smoothstep(
      Mth.clamp(baseIndex + (indexNoise.noise(x, z) - 0.5) * 0.08, 0.0, 1.0)
    );
  }

  protected double smoothstep(double t) {
    return t * t * (3.0 - 2.0 * t);
  }

  protected abstract double extractParameter(
    KoppenParameterCache.ParameterCombination params
  );

  protected double postProcessResult(double result) {
    return result;
  }
}
