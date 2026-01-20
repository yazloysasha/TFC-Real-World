package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.dries007.tfc.world.noise.Noise2D;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;

public abstract class BaseKoppenBasedNoise implements Noise2D {

  private static final double OFFSET = 0.1;

  protected static class CornerData {

    final double[] tempGrayscales = new double[4];
    final double[] rainGrayscales = new double[4];
    final KoppenParameterCache.ParameterCombination[] params =
      new KoppenParameterCache.ParameterCombination[4];
  }

  protected final PNGKoppenNoise koppenNoise;
  protected final PNGTemperatureNoise temperatureNoise;
  protected final PNGRainfallNoise rainfallNoise;
  protected final KoppenParameterCache parameterCache;

  private final ThreadLocal<CornerData> cornerDataCache =
    ThreadLocal.withInitial(CornerData::new);

  protected BaseKoppenBasedNoise(
    PNGKoppenNoise koppenNoise,
    PNGTemperatureNoise temperatureNoise,
    PNGRainfallNoise rainfallNoise
  ) {
    this.koppenNoise = koppenNoise;
    this.temperatureNoise = temperatureNoise;
    this.rainfallNoise = rainfallNoise;
    this.parameterCache = KoppenParameterCache.getInstance();
  }

  @Override
  public float noise(float x, float z) {
    PNGKoppenNoise.ClimateInterpolationResult interpolation =
      koppenNoise.getClimateInterpolation(x, z);

    CornerData data = cornerDataCache.get();
    sampleCornerData(x, z, interpolation, data);

    double result =
      extractParameter(data.params[0]) * interpolation.weight00 +
      extractParameter(data.params[1]) * interpolation.weight10 +
      extractParameter(data.params[2]) * interpolation.weight01 +
      extractParameter(data.params[3]) * interpolation.weight11;

    return (float) postProcessResult(result);
  }

  protected void sampleCornerData(
    double x,
    double z,
    PNGKoppenNoise.ClimateInterpolationResult interpolation,
    CornerData data
  ) {
    data.tempGrayscales[0] = temperatureNoise.getGrayscaleValue(
      x - OFFSET,
      z - OFFSET
    );
    data.tempGrayscales[1] = temperatureNoise.getGrayscaleValue(
      x + OFFSET,
      z - OFFSET
    );
    data.tempGrayscales[2] = temperatureNoise.getGrayscaleValue(
      x - OFFSET,
      z + OFFSET
    );
    data.tempGrayscales[3] = temperatureNoise.getGrayscaleValue(
      x + OFFSET,
      z + OFFSET
    );

    data.rainGrayscales[0] = rainfallNoise.getGrayscaleValue(
      x - OFFSET,
      z - OFFSET
    );
    data.rainGrayscales[1] = rainfallNoise.getGrayscaleValue(
      x + OFFSET,
      z - OFFSET
    );
    data.rainGrayscales[2] = rainfallNoise.getGrayscaleValue(
      x - OFFSET,
      z + OFFSET
    );
    data.rainGrayscales[3] = rainfallNoise.getGrayscaleValue(
      x + OFFSET,
      z + OFFSET
    );

    data.params[0] = parameterCache.getParametersFromGrayscale(
      interpolation.climate00,
      data.tempGrayscales[0],
      data.rainGrayscales[0]
    );
    data.params[1] = parameterCache.getParametersFromGrayscale(
      interpolation.climate10,
      data.tempGrayscales[1],
      data.rainGrayscales[1]
    );
    data.params[2] = parameterCache.getParametersFromGrayscale(
      interpolation.climate01,
      data.tempGrayscales[2],
      data.rainGrayscales[2]
    );
    data.params[3] = parameterCache.getParametersFromGrayscale(
      interpolation.climate11,
      data.tempGrayscales[3],
      data.rainGrayscales[3]
    );
  }

  protected abstract double extractParameter(
    KoppenParameterCache.ParameterCombination params
  );

  protected double postProcessResult(double result) {
    return result;
  }
}
