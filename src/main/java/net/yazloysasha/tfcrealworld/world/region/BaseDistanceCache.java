package net.yazloysasha.tfcrealworld.world.region;

import net.yazloysasha.tfcrealworld.world.noise.PNGContinentNoise;

/**
 * Base class for distance caches that share common functionality.
 * Provides coordinate transformation and ocean pixel detection.
 */
abstract class BaseDistanceCache {

  protected static final double CONTINENT_THRESHOLD = 4.4;

  protected final byte[] distanceMap;
  protected final int width;
  protected final int height;
  protected final double centerX;
  protected final double centerZ;
  protected final double scaleX;
  protected final double scaleZ;
  protected final double worldRadiusGridX;
  protected final double worldRadiusGridZ;
  protected final PNGContinentNoise continentNoise;

  protected BaseDistanceCache(PNGContinentNoise continentNoise) {
    this.continentNoise = continentNoise;
    this.width = continentNoise.getWidth();
    this.height = continentNoise.getHeight();
    this.centerX = continentNoise.getCenterX();
    this.centerZ = continentNoise.getCenterZ();
    this.scaleX = continentNoise.getScaleX();
    this.scaleZ = continentNoise.getScaleZ();
    this.worldRadiusGridX = continentNoise.getWorldRadiusGridX();
    this.worldRadiusGridZ = continentNoise.getWorldRadiusGridZ();

    this.distanceMap = new byte[width * height];
  }

  /**
   * Transforms world coordinates to image coordinates and performs bilinear interpolation.
   * Returns the four corner indices and interpolation factors.
   */
  protected InterpolationResult getInterpolationData(int gridX, int gridZ) {
    double clampedX = Math.clamp(gridX, -worldRadiusGridX, worldRadiusGridX);
    double clampedZ = Math.clamp(gridZ, -worldRadiusGridZ, worldRadiusGridZ);

    double imageX = centerX + clampedX * scaleX;
    double imageZ = centerZ + clampedZ * scaleZ;

    imageX = Math.clamp(imageX, 0, width - 1);
    imageZ = Math.clamp(imageZ, 0, height - 1);

    int x0 = (int) Math.floor(imageX);
    int z0 = (int) Math.floor(imageZ);
    int x1 = Math.min(x0 + 1, width - 1);
    int z1 = Math.min(z0 + 1, height - 1);

    double fx = imageX - x0;
    double fz = imageZ - z0;

    return new InterpolationResult(x0, z0, x1, z1, fx, fz);
  }

  protected boolean isOceanPixel(int x, int z) {
    if (x < 0 || x >= width || z < 0 || z >= height) {
      return false;
    }
    double brightness = continentNoise.getBrightness(x, z);
    double continentValue = (brightness / 255.0) * 10.0;
    return continentValue <= CONTINENT_THRESHOLD;
  }

  /**
   * Result of coordinate transformation for bilinear interpolation.
   */
  protected static class InterpolationResult {

    final int x0;
    final int z0;
    final int x1;
    final int z1;
    final double fx;
    final double fz;

    InterpolationResult(int x0, int z0, int x1, int z1, double fx, double fz) {
      this.x0 = x0;
      this.z0 = z0;
      this.x1 = x1;
      this.z1 = z1;
      this.fx = fx;
      this.fz = fz;
    }
  }
}
