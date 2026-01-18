package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;

public class KoppenBasedRainfallVarianceNoise extends BaseKoppenBasedNoise {

  public KoppenBasedRainfallVarianceNoise(
    PNGKoppenNoise koppenNoise,
    PNGTemperatureNoise temperatureNoise,
    PNGRainfallNoise rainfallNoise,
    long seed
  ) {
    super(koppenNoise, temperatureNoise, rainfallNoise);
  }

  @Override
  public double noise(double x, double z) {
    double[] image = temperatureNoise.tileToImage(x, z);
    double value = SmoothedKoppenParameterMaps.getInstance()
      .sampleRainVar(image[0], image[1]);
    return Mth.clamp(value, -1.0, 1.0);
  }

  @Override
  protected double extractParameter(
    KoppenParameterCache.ParameterCombination params
  ) {
    return params.rainVar;
  }
}
