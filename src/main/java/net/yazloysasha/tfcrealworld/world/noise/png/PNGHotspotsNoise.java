package net.yazloysasha.tfcrealworld.world.noise.png;

public class PNGHotspotsNoise extends BasePNGNoise {

  private static final String MAP_NAME = "hotspots";

  public PNGHotspotsNoise(int horizontalScale, int verticalScale) {
    super(
      horizontalScale,
      verticalScale,
      MAP_NAME,
      "Failed to load hotspots map. Map file is required when generating hotspots from map."
    );
  }

  @Override
  protected double transformBrightness(double brightness) {
    return brightness / 255.0;
  }

  public static byte ageFromBrightness(double brightness) {
    if (brightness <= 32.0) {
      return 0;
    }
    if (brightness <= 95.5) {
      return 4;
    }
    if (brightness <= 159.5) {
      return 3;
    }
    if (brightness <= 223.5) {
      return 2;
    }
    return 1;
  }

  public byte getHotSpotAge(double x, double z) {
    return ageFromBrightness(sampleBrightnessAtWorld(x, z));
  }

  public boolean hasActiveHotspot(double x, double z) {
    double[] imageCoords = tileToImage(x, z);
    int[] px = samplePixels(imageCoords[0], imageCoords[1]);
    for (int rgb : px) {
      if (getBrightness(rgb) > 223.5) return true;
    }
    return false;
  }
}
