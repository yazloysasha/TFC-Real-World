package net.yazloysasha.tfcrealworld.world.noise.png;

/**
 * {@code temperature.png}: TFC annual mean temperature, black = -25 °C,
 * white = 30 °C.
 */
public class PNGTemperatureNoise extends BaseClimatePNGNoise {

  public static final double MIN = -25.0;
  public static final double MAX = 30.0;

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
    return MIN + (brightness / 255.0) * (MAX - MIN);
  }
}
