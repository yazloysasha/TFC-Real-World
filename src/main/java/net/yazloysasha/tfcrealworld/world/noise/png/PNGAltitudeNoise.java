package net.yazloysasha.tfcrealworld.world.noise.png;

import net.minecraft.util.Mth;

public class PNGAltitudeNoise extends BasePNGNoise {

  private static final byte MIN_MAP_OCEAN_DEPTH = 2;

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
    final int x = (int) Math.round(Mth.clamp(imageX, 0, width - 1));
    final int z = (int) Math.round(Mth.clamp(imageZ, 0, height - 1));
    return getBrightness(pixels[z * width + x]);
  }

  public byte getBaseLandHeight(double x, double z) {
    double brightness = sampleBrightnessAtWorld(x, z);
    double height = transformBrightness(brightness);
    return (byte) Mth.clamp(Math.round(height), 0, 24);
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
    final int raw = (int) Mth.clamp(Math.round(depth), MIN_MAP_OCEAN_DEPTH, 15);
    if (raw == 5 || raw == 6) {
      return 2;
    }
    return (byte) raw;
  }
}
