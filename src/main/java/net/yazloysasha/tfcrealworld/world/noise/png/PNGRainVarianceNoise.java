package net.yazloysasha.tfcrealworld.world.noise.png;

import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;

/**
 * {@code rain_variance.png}: rainfall variance of the summer half of the
 * year, black = -1 (wet winter, dry summer), white = 1 (wet summer, dry
 * winter), mid gray = even rain. The same on both sides of the equator, so
 * the map has no jump there; TFC's variance follows the calendar, and takes
 * the sign of the hemisphere the map is read in.
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

  /** The hemisphere of a region point: that of its cell centre. */
  public static boolean isNorth(int gridZ) {
    return (
      ProjectionManager.resolveLatitudeFromWorldZ(
        Units.gridToBlock(gridZ) + Units.GRID_WIDTH_IN_BLOCK / 2
      ) >
      0
    );
  }

  /**
   * TFC's calendar variance of a region point from the map's summer variance.
   */
  public static float calendarVariance(double summerVariance, int gridZ) {
    final double variance = isNorth(gridZ) ? summerVariance : -summerVariance;
    return Math.clamp((float) variance, -1, 1);
  }
}
