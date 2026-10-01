package net.yazloysasha.tfcrealworld.world.noise.png;

/**
 * {@code rain_variance.png}: TFC rainfall variance, black = -1 (wet January,
 * dry July), white = 1 (dry January, wet July), mid gray = even rain.
 */
public class PNGRainVarianceNoise extends BaseClimatePNGNoise {

  public PNGRainVarianceNoise(int horizontalScale, int verticalScale) {
    super(
      horizontalScale,
      verticalScale,
      "rain_variance",
      "Failed to load rain_variance map. Map file is required when using PNG-based climate."
    );
  }

  @Override
  protected double transformBrightness(double brightness) {
    return -1.0 + (brightness / 255.0) * 2.0;
  }
}
