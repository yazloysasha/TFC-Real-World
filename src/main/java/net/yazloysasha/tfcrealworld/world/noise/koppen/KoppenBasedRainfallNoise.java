package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.minecraft.util.Mth;
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
  public float noise(float x, float z) {
    double[] image = temperatureNoise.tileToImage(x, z);
    double value = SmoothedKoppenParameterMaps.getInstance()
      .sampleRainfall(image[0], image[1]);
    return (float) Mth.clamp(
      value,
      ClimateConstants.RAIN_MIN,
      ClimateConstants.RAIN_MAX
    );
  }

  @Override
  protected double extractParameter(
    KoppenParameterCache.ParameterCombination params
  ) {
    return params.rainfall;
  }
}
