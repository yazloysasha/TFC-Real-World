package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.yazloysasha.tfcrealworld.world.climate.KoppenClimateCode;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;

public class KoppenBasedTemperatureNoise extends BaseKoppenBasedNoise {

  public KoppenBasedTemperatureNoise(
    PNGKoppenNoise koppenNoise,
    PNGTemperatureNoise temperatureNoise,
    PNGRainfallNoise rainfallNoise
  ) {
    super(koppenNoise, temperatureNoise, rainfallNoise);
  }

  @Override
  protected float getMinValue(KoppenClimateCode code) {
    return code.getMinTemp();
  }

  @Override
  protected float getMaxValue(KoppenClimateCode code) {
    return code.getMaxTemp();
  }

  @Override
  protected boolean isTemperature() {
    return true;
  }
}
