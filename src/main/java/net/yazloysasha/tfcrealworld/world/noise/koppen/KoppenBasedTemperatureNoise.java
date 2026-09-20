package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.yazloysasha.tfcrealworld.compat.TfeCompat;
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
  public double noise(double x, double z) {
    double[] image = temperatureNoise.tileToImage(x, z);
    if (TfeCompat.useTfeKoppenMaps()) {
      return TfeSmoothedKoppenParameterMaps.getInstance()
        .sampleTemperature(image[0], image[1]);
    }
    return SmoothedKoppenParameterMaps.getInstance()
      .sampleTemperature(image[0], image[1]);
  }

  @Override
  protected double extractParameter(
    KoppenParameterCache.ParameterCombination params
  ) {
    return params.temperature;
  }
}
