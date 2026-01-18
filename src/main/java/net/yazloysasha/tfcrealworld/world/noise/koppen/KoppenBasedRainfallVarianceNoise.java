package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;

public class KoppenBasedRainfallVarianceNoise extends BaseKoppenBasedNoise {

  private final Noise2D variationNoise;

  public KoppenBasedRainfallVarianceNoise(
    PNGKoppenNoise koppenNoise,
    PNGTemperatureNoise temperatureNoise,
    PNGRainfallNoise rainfallNoise,
    long seed
  ) {
    super(koppenNoise, temperatureNoise, rainfallNoise, seed, 0.1f);
    this.variationNoise = new OpenSimplex2D(seed + 99999L)
      .octaves(2)
      .spread(0.1f)
      .scaled(0.0, 1.0);
  }

  @Override
  public double noise(double x, double z) {
    PNGKoppenNoise.ClimateInterpolationResult interpolation =
      koppenNoise.getClimateInterpolation(x, z);

    double[] tempGrayscales = new double[] {
      temperatureNoise.getGrayscaleValue(x - 0.1, z - 0.1),
      temperatureNoise.getGrayscaleValue(x + 0.1, z - 0.1),
      temperatureNoise.getGrayscaleValue(x - 0.1, z + 0.1),
      temperatureNoise.getGrayscaleValue(x + 0.1, z + 0.1),
    };

    double[] rainGrayscales = new double[] {
      rainfallNoise.getGrayscaleValue(x - 0.1, z - 0.1),
      rainfallNoise.getGrayscaleValue(x + 0.1, z - 0.1),
      rainfallNoise.getGrayscaleValue(x - 0.1, z + 0.1),
      rainfallNoise.getGrayscaleValue(x + 0.1, z + 0.1),
    };

    KoppenParameterCache.ParameterCombination params00 =
      parameterCache.getParametersFromGrayscale(
        interpolation.climate00,
        tempGrayscales[0],
        rainGrayscales[0]
      );
    KoppenParameterCache.ParameterCombination params10 =
      parameterCache.getParametersFromGrayscale(
        interpolation.climate10,
        tempGrayscales[1],
        rainGrayscales[1]
      );
    KoppenParameterCache.ParameterCombination params01 =
      parameterCache.getParametersFromGrayscale(
        interpolation.climate01,
        tempGrayscales[2],
        rainGrayscales[2]
      );
    KoppenParameterCache.ParameterCombination params11 =
      parameterCache.getParametersFromGrayscale(
        interpolation.climate11,
        tempGrayscales[3],
        rainGrayscales[3]
      );

    double variation00 = variationNoise.noise(x - 0.1, z - 0.1);
    double variation10 = variationNoise.noise(x + 0.1, z - 0.1);
    double variation01 = variationNoise.noise(x - 0.1, z + 0.1);
    double variation11 = variationNoise.noise(x + 0.1, z + 0.1);

    float[] range00 = parameterCache.getRainVarRangeForCombination(
      interpolation.climate00,
      params00.temperature,
      params00.rainfall
    );
    float[] range10 = parameterCache.getRainVarRangeForCombination(
      interpolation.climate10,
      params10.temperature,
      params10.rainfall
    );
    float[] range01 = parameterCache.getRainVarRangeForCombination(
      interpolation.climate01,
      params01.temperature,
      params01.rainfall
    );
    float[] range11 = parameterCache.getRainVarRangeForCombination(
      interpolation.climate11,
      params11.temperature,
      params11.rainfall
    );

    double rainVar00 = range00[0] + (range00[1] - range00[0]) * variation00;
    double rainVar10 = range10[0] + (range10[1] - range10[0]) * variation10;
    double rainVar01 = range01[0] + (range01[1] - range01[0]) * variation01;
    double rainVar11 = range11[0] + (range11[1] - range11[0]) * variation11;

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
