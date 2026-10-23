package net.yazloysasha.tfcrealworld.world.noise.png;

import net.dries007.tfc.world.region.Units;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;

/**
 * {@code rain_variance.png}: rainfall variance of the summer half of the
 * year, black = -1 (wet winter, dry summer), white = 1 (wet summer, dry
 * winter), mid gray = even rain. The same on both sides of the equator, so
 * the map has no jump there; TFC's variance follows the calendar, and takes
 * the sign of the hemisphere the map is read in. Near the equator that sign
 * fades through zero, as rain there comes evenly round the year: a sign
 * turned at the equator itself splits rain, groundwater and grass colour
 * along it.
 */
public class PNGRainVarianceNoise extends BaseClimatePNGNoise {

  /** Latitude, either side of the equator, over which the sign fades. */
  private static final double EQUATORIAL_BAND_DEGREES = 10.0;

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

  /**
   * The hemisphere of a region point, from its cell centre: 1 north, -1 south,
   * in between near the equator.
   */
  public static double hemisphere(int gridZ) {
    final double latitude = ProjectionManager.resolveLatitudeFromWorldZ(
      RegionCoords.gridToBlock(gridZ) + Units.GRID_WIDTH_IN_BLOCK / 2
    );
    return Mth.clamp(latitude / EQUATORIAL_BAND_DEGREES, -1, 1);
  }

  /**
   * TFC's calendar variance of a region point from the map's summer variance.
   */
  public static float calendarVariance(double summerVariance, int gridZ) {
    return Mth.clamp((float) (hemisphere(gridZ) * summerVariance), -1, 1);
  }
}
