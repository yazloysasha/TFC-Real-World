package net.yazloysasha.tfcrealworld.world.noise.png;

public class PNGAltitudeNoise extends BasePNGNoise {

  public static final byte ABYSSAL_OCEAN_DEPTH = 7;
  public static final byte REEF_OCEAN_DEPTH = 1;
  public static final byte SHELF_OCEAN_DEPTH = 2;
  public static final byte MIN_MAP_OCEAN_DEPTH = 2;
  public static final int MAP_OCEAN_TRENCH_RAW_DEPTH = 10;

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

  @Override
  protected double transformBrightness(double brightness) {
    if (brightness < SEA_LEVEL_GRAYSCALE) {
      return 0.0;
    }

    double normalized =
      (brightness - SEA_LEVEL_GRAYSCALE) / (255.0 - SEA_LEVEL_GRAYSCALE);

    return normalized * 24.0;
  }

  @Override
  protected double sampleBrightness(double imageX, double imageZ) {
    final int x = (int) Math.round(Math.clamp(imageX, 0, width - 1));
    final int z = (int) Math.round(Math.clamp(imageZ, 0, height - 1));
    return getBrightness(pixels[z * width + x]);
  }

  public byte getBaseLandHeight(double x, double z) {
    double brightness = sampleBrightnessAtWorld(x, z);
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
    double brightness = sampleBrightnessAtWorld(x, z);
    double depth = transformOceanDepth(brightness);
    if (depth <= 0) {
      return 0;
    }
    return (byte) Math.clamp(Math.round(depth), MIN_MAP_OCEAN_DEPTH, 15);
  }

  public static byte normalizeMapOceanDepth(byte depth) {
    final int raw = Byte.toUnsignedInt(depth);
    if (raw <= 0) {
      return 0;
    }
    if (raw <= 5) {
      return SHELF_OCEAN_DEPTH;
    }
    return depth;
  }

  public static byte bucketFromRawOceanDepth(int rawDepth) {
    if (rawDepth <= 0) {
      return 0;
    }
    if (rawDepth <= 1) {
      return 1;
    }
    if (rawDepth <= 5) {
      return 2;
    }
    if (rawDepth <= 7) {
      return 4;
    }
    if (rawDepth <= 13) {
      return ABYSSAL_OCEAN_DEPTH;
    }
    return 4;
  }

  public AltitudeResult getAltitude(double x, double z) {
    double brightness = sampleBrightnessAtWorld(x, z);
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

  public static class AltitudeResult {

    public final byte landHeight;
    public final byte oceanDepth;

    public AltitudeResult(byte landHeight, byte oceanDepth) {
      this.landHeight = landHeight;
      this.oceanDepth = oceanDepth;
    }
  }
}
