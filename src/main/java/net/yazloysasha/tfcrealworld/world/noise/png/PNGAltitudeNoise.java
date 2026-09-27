package net.yazloysasha.tfcrealworld.world.noise.png;

/**
 * Altitude PNG supplies:
 * <ul>
 *   <li>continuous continent-noise-like values (0–10) for vanilla
 *       {@code AddContinentsAndSetOceanDepths} shelf / trench buckets</li>
 *   <li>{@code baseLandHeight} on land</li>
 * </ul>
 * Land vs ocean membership itself comes from {@code continent.png}, not from
 * this continuum.
 */
public class PNGAltitudeNoise extends BasePNGNoise {

  private static final String MAP_NAME = "altitude";
  private static final double SEA_LEVEL_GRAYSCALE = 128.0;

  public PNGAltitudeNoise(int horizontalScale, int verticalScale) {
    super(
      horizontalScale,
      verticalScale,
      MAP_NAME,
      "Failed to load altitude map. Map file is required when generating altitude from map."
    );
  }

  /**
   * Map grayscale → vanilla continentNoise scale (0–10). Same linear mapping
   * formerly applied to {@code continent.png}; shelf (3.3) / land (4.4)
   * thresholds apply to this value, not to the binary land mask.
   */
  @Override
  protected double transformBrightness(double brightness) {
    return (brightness / 255.0) * 10.0;
  }

  @Override
  protected double sampleBrightness(double imageX, double imageZ) {
    final int x = (int) Math.round(Math.clamp(imageX, 0, width - 1));
    final int z = (int) Math.round(Math.clamp(imageZ, 0, height - 1));
    return getBrightness(pixels[z * width + x]);
  }

  /**
   * Continuous continent-noise-like sample for AddContinents oceanDepth buckets.
   */
  public double getContinentNoise(double x, double z) {
    return noise(x, z);
  }

  /**
   * Land elevation 0–24 from grayscale at/above sea level (128–255).
   */
  public byte getBaseLandHeight(double x, double z) {
    double brightness = sampleBrightnessAtWorld(x, z);
    if (brightness < SEA_LEVEL_GRAYSCALE) {
      return 0;
    }
    double normalized =
      (brightness - SEA_LEVEL_GRAYSCALE) / (255.0 - SEA_LEVEL_GRAYSCALE);
    double height = normalized * 24.0;
    return (byte) Math.clamp(Math.round(height), 0, 24);
  }
}
