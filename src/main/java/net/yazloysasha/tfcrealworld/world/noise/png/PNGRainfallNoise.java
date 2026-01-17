package net.yazloysasha.tfcrealworld.world.noise.png;

public class PNGRainfallNoise extends BasePNGNoise {

  public PNGRainfallNoise(int horizontalTileSize, int verticalTileSize) {
    super(
      horizontalTileSize,
      verticalTileSize,
      "rainfall",
      "Failed to load rainfall map. Map file is required when using PNG-based rainfall."
    );
  }

  @Override
  protected double transformBrightness(double brightness) {
    return brightness;
  }

  public double getGrayscaleValue(double x, double z) {
    double[] imageCoords = tileToImage(x, z);
    return sampleBrightness(imageCoords[0], imageCoords[1]);
  }
}
