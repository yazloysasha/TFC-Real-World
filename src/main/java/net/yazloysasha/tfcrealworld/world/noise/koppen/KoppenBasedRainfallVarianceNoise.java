package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;

public class KoppenBasedRainfallVarianceNoise extends BaseKoppenBasedNoise {

  private static final double OFFSET = 0.1;

  private final Noise2D variationNoise;
  private final ThreadLocal<double[]> variationsCache = ThreadLocal.withInitial(
    () -> new double[4]
  );
  private final ThreadLocal<float[][]> rangesCache = ThreadLocal.withInitial(
    () -> new float[4][]
  );

  public KoppenBasedRainfallVarianceNoise(
    PNGKoppenNoise koppenNoise,
    PNGTemperatureNoise temperatureNoise,
    PNGRainfallNoise rainfallNoise,
    long seed
  ) {
    super(koppenNoise, temperatureNoise, rainfallNoise);
    this.variationNoise = new OpenSimplex2D(seed + 99999L)
      .octaves(2)
      .spread(0.1f)
      .scaled(0.0, 1.0);
  }

  @Override
  public double noise(double x, double z) {
    PNGKoppenNoise.ClimateInterpolationResult interpolation =
      koppenNoise.getClimateInterpolation(x, z);

    CornerData data = new CornerData();
    sampleCornerData(x, z, interpolation, data);

    double[] variations = variationsCache.get();
    variations[0] = variationNoise.noise(x - OFFSET, z - OFFSET);
    variations[1] = variationNoise.noise(x + OFFSET, z - OFFSET);
    variations[2] = variationNoise.noise(x - OFFSET, z + OFFSET);
    variations[3] = variationNoise.noise(x + OFFSET, z + OFFSET);

    float[][] ranges = rangesCache.get();
    ranges[0] = parameterCache.getRainVarRangeForCombination(
      interpolation.climate00,
      data.params[0].temperature,
      data.params[0].rainfall
    );
    ranges[1] = parameterCache.getRainVarRangeForCombination(
      interpolation.climate10,
      data.params[1].temperature,
      data.params[1].rainfall
    );
    ranges[2] = parameterCache.getRainVarRangeForCombination(
      interpolation.climate01,
      data.params[2].temperature,
      data.params[2].rainfall
    );
    ranges[3] = parameterCache.getRainVarRangeForCombination(
      interpolation.climate11,
      data.params[3].temperature,
      data.params[3].rainfall
    );

    double result =
      lerp(ranges[0], variations[0]) * interpolation.weight00 +
      lerp(ranges[1], variations[1]) * interpolation.weight10 +
      lerp(ranges[2], variations[2]) * interpolation.weight01 +
      lerp(ranges[3], variations[3]) * interpolation.weight11;

    return Mth.clamp(result, -1.0, 1.0);
  }

  private double lerp(float[] range, double t) {
    return range[0] + (range[1] - range[0]) * t;
  }

  @Override
  protected double extractParameter(
    KoppenParameterCache.ParameterCombination params
  ) {
    return params.rainVar;
  }
}
