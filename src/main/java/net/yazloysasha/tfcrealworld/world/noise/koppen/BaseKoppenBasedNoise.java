package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;

public abstract class BaseKoppenBasedNoise implements Noise2D {

  protected final PNGKoppenNoise koppenNoise;
  protected final PNGTemperatureNoise temperatureNoise;
  protected final PNGRainfallNoise rainfallNoise;
  protected final KoppenParameterCache parameterCache;
  protected final Noise2D variationNoise;
  protected final Noise2D indexNoise;

  protected BaseKoppenBasedNoise(
    PNGKoppenNoise koppenNoise,
    PNGTemperatureNoise temperatureNoise,
    PNGRainfallNoise rainfallNoise,
    long seed,
    float spread
  ) {
    this.koppenNoise = koppenNoise;
    this.temperatureNoise = temperatureNoise;
    this.rainfallNoise = rainfallNoise;
    this.parameterCache = KoppenParameterCache.getInstance();

    this.variationNoise = new OpenSimplex2D(seed + 12345L)
      .octaves(2)
      .spread(spread)
      .scaled(-1.0, 1.0);

    this.indexNoise = new OpenSimplex2D(seed)
      .octaves(2)
      .spread(spread)
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

    double variationAmplitude = 32.0;
    double[] tempVariations = new double[] {
      variationNoise.noise(x - 0.1, z - 0.1) * variationAmplitude,
      variationNoise.noise(x + 0.1, z - 0.1) * variationAmplitude,
      variationNoise.noise(x - 0.1, z + 0.1) * variationAmplitude,
      variationNoise.noise(x + 0.1, z + 0.1) * variationAmplitude,
    };

    double[] rainVariations = new double[] {
      variationNoise.noise(x - 0.1 + 1000, z - 0.1 + 1000) * variationAmplitude,
      variationNoise.noise(x + 0.1 + 1000, z - 0.1 + 1000) * variationAmplitude,
      variationNoise.noise(x - 0.1 + 1000, z + 0.1 + 1000) * variationAmplitude,
      variationNoise.noise(x + 0.1 + 1000, z + 0.1 + 1000) * variationAmplitude,
    };

    for (int i = 0; i < 4; i++) {
      tempGrayscales[i] = Mth.clamp(
        tempGrayscales[i] + tempVariations[i],
        0.0,
        255.0
      );
      rainGrayscales[i] = Mth.clamp(
        rainGrayscales[i] + rainVariations[i],
        0.0,
        255.0
      );
    }

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
