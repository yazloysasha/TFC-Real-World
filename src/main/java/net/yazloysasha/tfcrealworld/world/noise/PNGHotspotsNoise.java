package net.yazloysasha.tfcrealworld.world.noise;

public class PNGHotspotsNoise extends BasePNGNoise {

  private static final String MAP_NAME = "hotspots";

  public PNGHotspotsNoise(int horizontalWorldScale, int verticalWorldScale) {
    super(
      horizontalWorldScale,
      verticalWorldScale,
      MAP_NAME,
      "Failed to load hotspots map. Map file is required when generating hotspots from map."
    );
  }

  @Override
  protected double transformBrightness(double brightness) {
    return brightness / 255.0;
  }

  public byte getHotSpotAge(double x, double z) {
    double[] imageCoords = worldToImage(x, z);
    double brightness = sampleBrightness(imageCoords[0], imageCoords[1]);

    if (brightness <= 32.0) {
      return 0;
    } else if (brightness <= 95.5) {
      return 4;
    } else if (brightness <= 159.5) {
      return 3;
    } else if (brightness <= 223.5) {
      return 2;
    } else {
      return 1;
    }
  }

  public boolean hasHotspot(double x, double z) {
    return getHotSpotAge(x, z) > 0;
  }
}
