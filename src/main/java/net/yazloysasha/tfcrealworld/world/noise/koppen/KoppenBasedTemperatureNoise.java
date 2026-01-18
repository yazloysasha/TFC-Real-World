package net.yazloysasha.tfcrealworld.world.noise.koppen;

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
  protected double extractParameter(
    KoppenParameterCache.ParameterCombination params
  ) {
    return params.temperature;
  }
}
