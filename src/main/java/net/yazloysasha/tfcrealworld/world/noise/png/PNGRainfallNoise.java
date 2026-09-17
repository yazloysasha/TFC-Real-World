package net.yazloysasha.tfcrealworld.world.noise.png;

public class PNGRainfallNoise extends BasePNGNoise {

  public PNGRainfallNoise(int horizontalScale, int verticalScale) {
    super(
      horizontalScale,
      verticalScale,
      "rainfall",
      "Failed to load rainfall map. Map file is required when using PNG-based rainfall."
    );
  }

  @Override
  protected double transformBrightness(double brightness) {
    return brightness;
  }

  public double getGrayscaleValue(double x, double z) {
    return sampleBrightnessAtWorld(x, z);
  }
}
