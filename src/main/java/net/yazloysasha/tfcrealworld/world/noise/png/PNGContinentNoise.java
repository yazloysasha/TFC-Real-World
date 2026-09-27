package net.yazloysasha.tfcrealworld.world.noise.png;

/**
 * {@code continent.png} is a binary land/ocean mask only. Continuous
 * continent-noise values for shelf / trench buckets come from
 * {@link PNGAltitudeNoise}, not from this map's brightness.
 */
public class PNGContinentNoise extends BasePNGNoise {

  private static final String MAP_NAME = "continent";

  /** Grayscale above this → land; at or below → ocean (map is 0 / 255). */
  public static final int LAND_MASK_THRESHOLD = 127;

  /** Hard land sample in the 0–10 continent-noise scale. */
  public static final double LAND_NOISE = 10.0;

  /** Hard ocean sample in the 0–10 continent-noise scale. */
  public static final double OCEAN_NOISE = 0.0;

  public PNGContinentNoise(int horizontalScale, int verticalScale) {
    super(
      horizontalScale,
      verticalScale,
      MAP_NAME,
      "Failed to load continent map. Map file is required when generating continents from map."
    );
  }

  @Override
  protected double transformBrightness(double brightness) {
    return brightness > LAND_MASK_THRESHOLD ? LAND_NOISE : OCEAN_NOISE;
  }

  /**
   * Hard land/ocean sample from the binary mask (not the 4.4 continuum).
   */
  public boolean isLandAtGridHard(double gridX, double gridZ) {
    return sampleGrayAtWorldRounded(gridX, gridZ) > LAND_MASK_THRESHOLD;
  }

  /**
   * Pixel-space land test for distance caches (same threshold as
   * {@link #isLandAtGridHard}).
   */
  public boolean isLandPixel(int x, int z) {
    return getBrightness(x, z) > LAND_MASK_THRESHOLD;
  }
}
