package net.yazloysasha.tfcrealworld.world.region;

/**
 * The climate maps are drawn in TFC 4's temperature scale; TFC 2 draws its
 * climate borders at other temperatures.
 */
public final class ClimateScale {

  /**
   * Annual mean temperatures at which TFC 4 and TFC 2 draw the same climate
   * border: ice cap, tundra, subarctic, subtropical, tropical. Between the
   * borders the scale is stretched evenly, and past them it is shifted.
   */
  private static final float[] TFC4_TEMPERATURES = { -17, -12, -2, 17, 21, 30 };
  private static final float[] TFC2_TEMPERATURES = { -20, -14, -5, 12, 18, 30 };

  private ClimateScale() {}

  /** The annual mean temperature of the climate maps in TFC 2's scale. */
  public static float temperature(float mapTemperature) {
    final int last = TFC4_TEMPERATURES.length - 1;
    if (mapTemperature <= TFC4_TEMPERATURES[0]) {
      return mapTemperature + TFC2_TEMPERATURES[0] - TFC4_TEMPERATURES[0];
    }
    if (mapTemperature >= TFC4_TEMPERATURES[last]) {
      return mapTemperature + TFC2_TEMPERATURES[last] - TFC4_TEMPERATURES[last];
    }
    int border = 1;
    while (mapTemperature > TFC4_TEMPERATURES[border]) {
      border++;
    }
    final float share =
      (mapTemperature - TFC4_TEMPERATURES[border - 1]) /
      (TFC4_TEMPERATURES[border] - TFC4_TEMPERATURES[border - 1]);
    return (
      TFC2_TEMPERATURES[border - 1] +
      share * (TFC2_TEMPERATURES[border] - TFC2_TEMPERATURES[border - 1])
    );
  }
}
