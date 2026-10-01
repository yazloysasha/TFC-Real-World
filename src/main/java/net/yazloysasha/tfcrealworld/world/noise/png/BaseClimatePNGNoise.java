package net.yazloysasha.tfcrealworld.world.noise.png;

/**
 * A climate map read by region points. A region point stands for the cell
 * [x, x + 1): it reads the cell centre, as the continent and tectonics maps
 * do, not the cell corner.
 */
public abstract class BaseClimatePNGNoise extends BasePNGNoise {

  protected BaseClimatePNGNoise(
    int horizontalScale,
    int verticalScale,
    String mapName,
    String errorMessage
  ) {
    super(horizontalScale, verticalScale, mapName, errorMessage);
  }

  @Override
  public double noise(double x, double z) {
    return super.noise(x + 0.5, z + 0.5);
  }
}
