package net.yazloysasha.tfcrealworld.world.noise.png;

/**
 * A climate map read by region points. A region point stands for the cell
 * [x, x + 1) and takes the mean of the cell's pixels.
 */
public abstract class BaseClimatePNGNoise extends BasePNGNoise {

  /** Samples per cell side: one per pixel of the cell. */
  private final int samplesX;
  private final int samplesZ;

  protected BaseClimatePNGNoise(
    int horizontalScale,
    int verticalScale,
    String mapName,
    String errorMessage
  ) {
    super(horizontalScale, verticalScale, mapName, errorMessage);
    this.samplesX = Math.max(1, (int) Math.round(scaleX));
    this.samplesZ = Math.max(1, (int) Math.round(scaleZ));
  }

  @Override
  public double noise(double x, double z) {
    double sum = 0;
    for (int stepZ = 0; stepZ < samplesZ; stepZ++) {
      for (int stepX = 0; stepX < samplesX; stepX++) {
        sum += atPixelCentres(
          x + (stepX + 0.5) / samplesX,
          z + (stepZ + 0.5) / samplesZ
        );
      }
    }
    return sum / (samplesX * samplesZ);
  }

  /** The map at a position in grid cells, blended between pixel centres. */
  private double atPixelCentres(double x, double z) {
    final double[] imageCoords = tileImageScratch();
    fillTileImageCoords(x, z, imageCoords);
    return transformBrightness(
      sampleBrightness(
        Math.max(imageCoords[0] - 0.5, 0),
        Math.max(imageCoords[1] - 0.5, 0)
      )
    );
  }
}
