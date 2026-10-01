package net.yazloysasha.tfcrealworld.world.noise.png;

/**
 * {@code rainfall.png}: TFC annual rainfall, black = 0 mm, white = 500 mm.
 */
public class PNGRainfallNoise extends BaseClimatePNGNoise {

  public static final double MAX = 500.0;

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
    return (brightness / 255.0) * MAX;
  }
}
