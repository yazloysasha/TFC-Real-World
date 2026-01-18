package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.dries007.tfc.world.noise.Noise2D;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.world.climate.KoppenClimateCode;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;

public abstract class BaseKoppenBasedNoise implements Noise2D {

  protected final PNGKoppenNoise koppenNoise;
  protected final PNGTemperatureNoise temperatureNoise;
  protected final PNGRainfallNoise rainfallNoise;

  protected BaseKoppenBasedNoise(
    PNGKoppenNoise koppenNoise,
    PNGTemperatureNoise temperatureNoise,
    PNGRainfallNoise rainfallNoise
  ) {
    this.koppenNoise = koppenNoise;
    this.temperatureNoise = temperatureNoise;
    this.rainfallNoise = rainfallNoise;
  }

  @Override
  public double noise(double x, double z) {
    PNGKoppenNoise.ClimateInterpolationResult interpolation =
      koppenNoise.getClimateInterpolation(x, z);

    double temp00 = temperatureNoise.getGrayscaleValue(x, z) / 255.0;
    double rain00 = rainfallNoise.getGrayscaleValue(x, z) / 255.0;

    float value00 = getValue(interpolation.climate00, temp00, rain00);
    float value10 = getValue(interpolation.climate10, temp00, rain00);
    float value01 = getValue(interpolation.climate01, temp00, rain00);
    float value11 = getValue(interpolation.climate11, temp00, rain00);

    double result =
      value00 * interpolation.weight00 +
      value10 * interpolation.weight10 +
      value01 * interpolation.weight01 +
      value11 * interpolation.weight11;

    return result;
  }

  protected float getValue(
    KoppenClimateCode code,
    double tempNorm,
    double rainNorm
  ) {
    float minValue = getMinValue(code);
    float maxValue = getMaxValue(code);
    float norm = (float) (isTemperature() ? tempNorm : rainNorm);
    return Mth.lerp(norm, minValue, maxValue);
  }

  protected abstract float getMinValue(KoppenClimateCode code);

  protected abstract float getMaxValue(KoppenClimateCode code);

  protected abstract boolean isTemperature();
}
