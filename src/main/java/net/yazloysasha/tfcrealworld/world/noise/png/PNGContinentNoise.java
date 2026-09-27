package net.yazloysasha.tfcrealworld.world.noise.png;

import net.yazloysasha.tfcrealworld.world.region.TfcContinentNoiseThresholds;

public class PNGContinentNoise extends BasePNGNoise {

  private static final String MAP_NAME = "continent";

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
    return (brightness / 255.0) * 10.0;
  }

  /**
   * Hard land/ocean sample using the same cutoff as TFC AddContinents.
   */
  public boolean isLandAtGridHard(double gridX, double gridZ) {
    final int gray = sampleGrayAtWorldRounded(gridX, gridZ);
    return transformBrightness(gray) > TfcContinentNoiseThresholds.LAND;
  }
}
