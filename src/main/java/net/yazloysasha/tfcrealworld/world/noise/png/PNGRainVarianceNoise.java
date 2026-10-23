package net.yazloysasha.tfcrealworld.world.noise.png;

/**
 * {@code rain_variance.png}: TFC's calendar rainfall variance, black = -1
 * (wet January, dry July), white = 1 (dry January, wet July), mid gray = even
 * rain. Read as it is in both hemispheres: on the real Earth it turns from
 * one sign to the other where the tropical rain belt lies on average, a
 * winding line near the equator, and the map keeps that line.
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

  /** TFC's calendar variance of a region point from the map's. */
  public static float calendarVariance(double mapVariance) {
    return Math.clamp((float) mapVariance, -1, 1);
  }
}
