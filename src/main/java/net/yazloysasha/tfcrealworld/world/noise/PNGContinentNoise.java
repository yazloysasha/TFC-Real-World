package net.yazloysasha.tfcrealworld.world.noise;

public class PNGContinentNoise extends BasePNGNoise {

  private static final String MAP_NAME = "continent";

  public PNGContinentNoise(int horizontalWorldScale, int verticalWorldScale) {
    super(
      horizontalWorldScale,
      verticalWorldScale,
      MAP_NAME,
      "Failed to load continent map. Map file is required when generating continents from map."
    );
  }

  @Override
  protected double transformBrightness(double brightness) {
    return (brightness / 255.0) * 10.0;
  }
}
