package net.yazloysasha.tfcrealworld.world.noise;

import com.mojang.logging.LogUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.util.MapPathHelper;
import org.slf4j.Logger;

public abstract class BasePNGNoise implements Noise2D {

  protected static final Logger LOGGER = LogUtils.getLogger();

  private static final Map<String, BufferedImage> imageCache = new HashMap<>();

  protected final int[] pixels;
  protected final int width;
  protected final int height;
  protected final double centerX;
  protected final double centerZ;
  protected final double scaleX;
  protected final double scaleZ;
  protected final int worldRadiusBlocksX;
  protected final int worldRadiusBlocksZ;
  protected final double worldRadiusGridX;
  protected final double worldRadiusGridZ;

  protected BasePNGNoise(
    int horizontalWorldSize,
    int verticalWorldSize,
    String mapName,
    String errorMessage
  ) {
    this.worldRadiusBlocksX = horizontalWorldSize / 2;
    this.worldRadiusBlocksZ = verticalWorldSize / 2;
    this.worldRadiusGridX =
      worldRadiusBlocksX / (double) Units.GRID_WIDTH_IN_BLOCK;
    this.worldRadiusGridZ =
      worldRadiusBlocksZ / (double) Units.GRID_WIDTH_IN_BLOCK;

    BufferedImage image = loadImage(mapName);
    if (image == null) {
      throw new RuntimeException(errorMessage);
    }

    this.width = image.getWidth();
    this.height = image.getHeight();
    this.pixels = new int[width * height];

    image.getRGB(0, 0, width, height, pixels, 0, width);

    this.centerX = width / 2.0;
    this.centerZ = height / 2.0;

    this.scaleX = width / (2.0 * worldRadiusGridX);
    this.scaleZ = height / (2.0 * worldRadiusGridZ);

    LOGGER.info(
      "Loaded {} map: {}x{} pixels, covering {}x{} blocks (radius X: {} blocks, Z: {} blocks)",
      mapName,
      width,
      height,
      worldRadiusBlocksX * 2,
      worldRadiusBlocksZ * 2,
      worldRadiusBlocksX,
      worldRadiusBlocksZ
    );
  }

  @Override
  public double noise(double x, double z) {
    double[] imageCoords = worldToImage(x, z);
    double brightness = sampleBrightness(imageCoords[0], imageCoords[1]);
    return transformBrightness(brightness);
  }

  protected InterpolationCoords calculateInterpolationCoords(
    double imageX,
    double imageZ
  ) {
    int x0 = (int) Math.floor(imageX);
    int z0 = (int) Math.floor(imageZ);
    int x1 = Math.min(x0 + 1, width - 1);
    int z1 = Math.min(z0 + 1, height - 1);

    double fx = imageX - x0;
    double fz = imageZ - z0;

    return new InterpolationCoords(x0, z0, x1, z1, fx, fz);
  }

  protected double sampleBrightness(double imageX, double imageZ) {
    InterpolationCoords coords = calculateInterpolationCoords(imageX, imageZ);

    double brightness00 = getBrightness(pixels[coords.z0 * width + coords.x0]);
    double brightness10 = getBrightness(pixels[coords.z0 * width + coords.x1]);
    double brightness01 = getBrightness(pixels[coords.z1 * width + coords.x0]);
    double brightness11 = getBrightness(pixels[coords.z1 * width + coords.x1]);

    double brightness0 =
      brightness00 * (1 - coords.fx) + brightness10 * coords.fx;
    double brightness1 =
      brightness01 * (1 - coords.fx) + brightness11 * coords.fx;
    return brightness0 * (1 - coords.fz) + brightness1 * coords.fz;
  }

  protected int[] samplePixels(double imageX, double imageZ) {
    InterpolationCoords coords = calculateInterpolationCoords(imageX, imageZ);

    return new int[] {
      pixels[coords.z0 * width + coords.x0],
      pixels[coords.z0 * width + coords.x1],
      pixels[coords.z1 * width + coords.x0],
      pixels[coords.z1 * width + coords.x1],
    };
  }

  protected static class InterpolationCoords {

    final int x0;
    final int z0;
    final int x1;
    final int z1;
    final double fx;
    final double fz;

    InterpolationCoords(int x0, int z0, int x1, int z1, double fx, double fz) {
      this.x0 = x0;
      this.z0 = z0;
      this.x1 = x1;
      this.z1 = z1;
      this.fx = fx;
      this.fz = fz;
    }
  }

  public double[] worldToImage(double x, double z) {
    double clampedX = Math.clamp(x, -worldRadiusGridX, worldRadiusGridX);
    double clampedZ = Math.clamp(z, -worldRadiusGridZ, worldRadiusGridZ);

    double imageX = centerX + clampedX * scaleX;
    double imageZ = centerZ + clampedZ * scaleZ;

    imageX = Math.clamp(imageX, 0, width - 1);
    imageZ = Math.clamp(imageZ, 0, height - 1);

    return new double[] { imageX, imageZ };
  }

  protected abstract double transformBrightness(double brightness);

  protected double getBrightness(int rgb) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    return 0.299 * r + 0.587 * g + 0.114 * b;
  }

  public double getBrightness(int x, int z) {
    if (x < 0 || x >= width || z < 0 || z >= height) {
      return 0.0;
    }
    return getBrightness(pixels[z * width + x]);
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public double getCenterX() {
    return centerX;
  }

  public double getCenterZ() {
    return centerZ;
  }

  public double getScaleX() {
    return scaleX;
  }

  public double getScaleZ() {
    return scaleZ;
  }

  public double getWorldRadiusGridX() {
    return worldRadiusGridX;
  }

  public double getWorldRadiusGridZ() {
    return worldRadiusGridZ;
  }

  public static BufferedImage loadImage(String mapName) {
    synchronized (imageCache) {
      BufferedImage cached = imageCache.get(mapName);
      if (cached != null) {
        LOGGER.debug("Using cached {} map", mapName);
        return cached;
      }
    }

    Path mapPath = MapPathHelper.getMapPath(mapName);
    try {
      if (!Files.exists(mapPath)) {
        LOGGER.error("{} map not found at: {}", mapName, mapPath);
        return null;
      }
      BufferedImage image = ImageIO.read(mapPath.toFile());
      if (image != null) {
        synchronized (imageCache) {
          imageCache.put(mapName, image);
          LOGGER.debug("Cached {} map", mapName);
        }
      }
      return image;
    } catch (IOException e) {
      LOGGER.error("Failed to load {} map from: {}", mapName, mapPath, e);
      return null;
    }
  }

  public static void clearImageCache() {
    synchronized (imageCache) {
      imageCache.clear();
      LOGGER.info("Cleared PNG image cache");
    }
  }
}
