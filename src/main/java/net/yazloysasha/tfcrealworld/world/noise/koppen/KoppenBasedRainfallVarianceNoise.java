package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;

public class KoppenBasedRainfallVarianceNoise extends BaseKoppenBasedNoise {

  public KoppenBasedRainfallVarianceNoise(
    PNGKoppenNoise koppenNoise,
    PNGTemperatureNoise temperatureNoise,
    PNGRainfallNoise rainfallNoise,
    long seed
  ) {
    super(koppenNoise, temperatureNoise, rainfallNoise, seed, 0.1f);
  }

  @Override
  public double noise(double x, double z) {
    PNGKoppenNoise.ClimateInterpolationResult interpolation =
      koppenNoise.getClimateInterpolation(x, z);

    double[] indices = calculateIndices(x, z);

    float[] range00 = parameterCache.getRainVarRange(interpolation.climate00);
    float[] range10 = parameterCache.getRainVarRange(interpolation.climate10);
    float[] range01 = parameterCache.getRainVarRange(interpolation.climate01);
    float[] range11 = parameterCache.getRainVarRange(interpolation.climate11);

    double rainVar00 = range00[0] + (range00[1] - range00[0]) * indices[0];
    double rainVar10 = range10[0] + (range10[1] - range10[0]) * indices[1];
    double rainVar01 = range01[0] + (range01[1] - range01[0]) * indices[2];
    double rainVar11 = range11[0] + (range11[1] - range11[0]) * indices[3];

    double result =
      rainVar00 * interpolation.weight00 +
      rainVar10 * interpolation.weight10 +
      rainVar01 * interpolation.weight01 +
      rainVar11 * interpolation.weight11;

    return Mth.clamp(result, -1.0, 1.0);
  }

  @Override
  protected double extractParameter(
    KoppenParameterCache.ParameterCombination params
  ) {
    return params.rainVar;
  }
}
