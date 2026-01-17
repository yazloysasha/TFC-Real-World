package net.yazloysasha.tfcrealworld.world.noise.koppen;

import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;

public class KoppenBasedRainfallNoise extends BaseKoppenBasedNoise {

  public KoppenBasedRainfallNoise(
    PNGKoppenNoise koppenNoise,
    PNGTemperatureNoise temperatureNoise,
    PNGRainfallNoise rainfallNoise,
    long seed
  ) {
    super(koppenNoise, temperatureNoise, rainfallNoise, seed, 0.1f);
  }

  @Override
  protected double extractParameter(
    KoppenParameterCache.ParameterCombination params
  ) {
    return params.rainfall;
  }

  @Override
  protected double postProcessResult(double result) {
    return Mth.clamp(result, 0.0, 500.0);
  }
}
