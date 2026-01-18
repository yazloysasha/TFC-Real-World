package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.world.climate.KoppenClimateCode;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;

public class KoppenBasedRainfallNoise extends BaseKoppenBasedNoise {

  public KoppenBasedRainfallNoise(
    PNGKoppenNoise koppenNoise,
    PNGTemperatureNoise temperatureNoise,
    PNGRainfallNoise rainfallNoise
  ) {
    super(koppenNoise, temperatureNoise, rainfallNoise);
  }

  @Override
  protected float getMinValue(KoppenClimateCode code) {
    return code.getMinRainfall();
  }

  @Override
  protected float getMaxValue(KoppenClimateCode code) {
    return code.getMaxRainfall();
  }

  @Override
  protected boolean isTemperature() {
    return false;
  }

  @Override
  public double noise(double x, double z) {
    return Mth.clamp(super.noise(x, z), 0.0, 500.0);
  }
}
