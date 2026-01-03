package net.yazloysasha.tfcrealworld.world.noise;

public class PNGAltitudeNoise extends BasePNGNoise {

  private static final String MAP_NAME = "altitude";
  private static final double SEA_LEVEL_GRAYSCALE = 128.0;

  public PNGAltitudeNoise(int horizontalWorldScale, int verticalWorldScale) {
    super(
      horizontalWorldScale,
      verticalWorldScale,
      MAP_NAME,
      "Failed to load altitude map. Map file is required when generating altitude from map."
    );
  }

  @Override
  protected double transformBrightness(double brightness) {
    if (brightness < SEA_LEVEL_GRAYSCALE) {
      return 0.0;
    }

    double normalized =
      (brightness - SEA_LEVEL_GRAYSCALE) / (255.0 - SEA_LEVEL_GRAYSCALE);

    return normalized * 24.0;
  }

  public byte getBaseLandHeight(double x, double z) {
    double[] imageCoords = worldToImage(x, z);
    double brightness = sampleBrightness(imageCoords[0], imageCoords[1]);
    double height = transformBrightness(brightness);
    return (byte) Math.clamp(Math.round(height), 0, 24);
  }

  protected double transformOceanDepth(double brightness) {
    final double MAX_OCEAN_DEPTH = 15.0;
    final double MIN_OCEAN_DEPTH = 1.0;

    if (brightness >= SEA_LEVEL_GRAYSCALE) {
      return 0.0;
    }

    double normalized = 1.0 - (brightness + 1) / SEA_LEVEL_GRAYSCALE;
    return MIN_OCEAN_DEPTH + normalized * (MAX_OCEAN_DEPTH - MIN_OCEAN_DEPTH);
  }

  public byte getBaseOceanDepth(double x, double z) {
    double[] imageCoords = worldToImage(x, z);
    double brightness = sampleBrightness(imageCoords[0], imageCoords[1]);
    double depth = transformOceanDepth(brightness);
    return (byte) Math.clamp(Math.round(depth), 0, 15);
  }

  /**
   * Get both land height and ocean depth for the same coordinates efficiently.
   * Avoids duplicate coordinate transformation and brightness sampling.
   */
  public AltitudeResult getAltitude(double x, double z) {
    double[] imageCoords = worldToImage(x, z);
    double brightness = sampleBrightness(imageCoords[0], imageCoords[1]);
    byte landHeight = (byte) Math.clamp(
      Math.round(transformBrightness(brightness)),
      0,
      24
    );
    byte oceanDepth = (byte) Math.clamp(
      Math.round(transformOceanDepth(brightness)),
      0,
      15
    );
    return new AltitudeResult(landHeight, oceanDepth);
  }

  /**
   * Result containing both land height and ocean depth for the same coordinates.
   */
  public static class AltitudeResult {

    public final byte landHeight;
    public final byte oceanDepth;

    public AltitudeResult(byte landHeight, byte oceanDepth) {
      this.landHeight = landHeight;
      this.oceanDepth = oceanDepth;
    }
  }
}
