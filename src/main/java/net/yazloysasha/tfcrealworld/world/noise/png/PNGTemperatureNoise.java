package net.yazloysasha.tfcrealworld.world.noise.png;

public class PNGTemperatureNoise extends BasePNGNoise {

  public PNGTemperatureNoise(int horizontalScale, int verticalScale) {
    super(
      horizontalScale,
      verticalScale,
      "temperature",
      "Failed to load temperature map. Map file is required when using PNG-based temperature."
    );
  }

  @Override
  protected double transformBrightness(double brightness) {
    return brightness;
  }

  public double getGrayscaleValue(double x, double z) {
    double[] imageCoords = tileToImage(x, z);
    return sampleBrightness(imageCoords[0], imageCoords[1]);
  }
}
