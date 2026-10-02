package net.yazloysasha.tfcrealworld.world.noise.png;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.profile.ProfileManager;

public abstract class BasePNGNoise implements Noise2D {

  /**
   * Decoded maps by profile and name. Soft: a map is needed again only by a
   * new world or the map screen, and the continent map is tens of megabytes.
   */
  private static final Map<String, SoftReference<BufferedImage>> imageCache =
    new HashMap<>();

  private static final ThreadLocal<double[]> TILE_IMAGE_SCRATCH =
    ThreadLocal.withInitial(() -> new double[2]);

  /** Brightness of every pixel, 0 to 255, row by row. */
  protected final byte[] pixels;
  protected final int width;
  protected final int height;
  protected final double centerX;
  protected final double centerZ;
  protected final double scaleX;
  protected final double scaleZ;
  protected final int tileRadiusBlocksX;
  protected final int tileRadiusBlocksZ;
  protected final double tileRadiusGridX;
  protected final double tileRadiusGridZ;

  protected BasePNGNoise(
    int horizontalScale,
    int verticalScale,
    String mapName,
    String errorMessage
  ) {
    this.tileRadiusBlocksX = horizontalScale;
    this.tileRadiusBlocksZ = verticalScale;
    this.tileRadiusGridX =
      tileRadiusBlocksX / (double) Units.GRID_WIDTH_IN_BLOCK;
    this.tileRadiusGridZ =
      tileRadiusBlocksZ / (double) Units.GRID_WIDTH_IN_BLOCK;

    BufferedImage image = loadImage(mapName);
    if (image == null) {
      throw new RuntimeException(errorMessage);
    }

    this.width = image.getWidth();
    this.height = image.getHeight();
    this.pixels = readBrightness(image);

    this.centerX = width / 2.0;
    this.centerZ = height / 2.0;

    this.scaleX = width / (2.0 * tileRadiusGridX);
    this.scaleZ = height / (2.0 * tileRadiusGridZ);
  }

  @Override
  public double noise(double x, double z) {
    return transformBrightness(sampleBrightnessAtWorld(x, z));
  }

  protected double sampleBrightnessAtWorld(double x, double z) {
    final double[] imageCoords = tileImageScratch();
    fillTileImageCoords(x, z, imageCoords);
    return sampleBrightness(imageCoords[0], imageCoords[1]);
  }

  protected int sampleGrayAtWorldRounded(double x, double z) {
    return brightnessAt(pixelIndexAtWorldRounded(x, z));
  }

  /** Index of the pixel nearest to {@code (x, z)}, as hard samples read it. */
  public int pixelIndexAtWorldRounded(double x, double z) {
    final double[] imageCoords = tileImageScratch();
    fillTileImageCoords(x, z, imageCoords);
    final int ix = Math.clamp(Math.round(imageCoords[0]), 0, width - 1);
    final int iz = Math.clamp(Math.round(imageCoords[1]), 0, height - 1);
    return iz * width + ix;
  }

  protected double[] tileImageScratch() {
    return TILE_IMAGE_SCRATCH.get();
  }

  protected double sampleBrightness(double imageX, double imageZ) {
    final int x0 = (int) Math.floor(imageX);
    final int z0 = (int) Math.floor(imageZ);
    final int x1 = Math.min(x0 + 1, width - 1);
    final int z1 = Math.min(z0 + 1, height - 1);
    final double fx = imageX - x0;
    final double fz = imageZ - z0;
    final double top =
      brightnessAt(z0 * width + x0) * (1 - fx) +
      brightnessAt(z0 * width + x1) * fx;
    final double bottom =
      brightnessAt(z1 * width + x0) * (1 - fx) +
      brightnessAt(z1 * width + x1) * fx;
    return top * (1 - fz) + bottom * fz;
  }

  protected int brightnessAt(int pixel) {
    return pixels[pixel] & 0xFF;
  }

  public double[] tileToImage(double x, double z) {
    final double[] out = new double[2];
    fillTileImageCoords(x, z, out);
    return out;
  }

  protected void fillTileImageCoords(double x, double z, double[] out) {
    int tileX = (int) Math.floor(
      (x + tileRadiusGridX) / (2.0 * tileRadiusGridX)
    );
    int tileZ = (int) Math.floor(
      (z + tileRadiusGridZ) / (2.0 * tileRadiusGridZ)
    );

    double tileCenterX = tileX * 2.0 * tileRadiusGridX;
    double tileCenterZ = tileZ * 2.0 * tileRadiusGridZ;
    double localX = x - tileCenterX;
    double localZ = z - tileCenterZ;

    if (Math.floorMod(tileX, 2) != 0) {
      localX = -localX;
    }
    if (Math.floorMod(tileZ, 2) != 0) {
      localZ = -localZ;
    }

    double clampedX = Math.clamp(localX, -tileRadiusGridX, tileRadiusGridX);
    double clampedZ = Math.clamp(localZ, -tileRadiusGridZ, tileRadiusGridZ);

    out[0] = Math.clamp(centerX + clampedX * scaleX, 0, width - 1);
    out[1] = Math.clamp(centerZ + clampedZ * scaleZ, 0, height - 1);
  }

  protected abstract double transformBrightness(double brightness);

  /**
   * Gray samples are read raw: {@code ImageIO.getRGB} on gray PNGs applies
   * sRGB and skews mid-tones. Anything else is read as colour, row by row.
   */
  private static byte[] readBrightness(BufferedImage image) {
    final int width = image.getWidth();
    final int height = image.getHeight();
    final byte[] pixels = new byte[width * height];
    final Raster raster = image.getRaster();
    final boolean gray =
      raster.getNumBands() == 1 &&
      !(image.getColorModel() instanceof IndexColorModel);
    final int[] row = new int[width];
    for (int z = 0; z < height; z++) {
      if (gray) {
        raster.getSamples(0, z, width, 1, 0, row);
      } else {
        image.getRGB(0, z, width, 1, row, 0, width);
      }
      for (int x = 0; x < width; x++) {
        pixels[z * width + x] = (byte) (gray ? row[x] : brightness(row[x]));
      }
    }
    return pixels;
  }

  private static int brightness(int rgb) {
    final int r = (rgb >> 16) & 0xFF;
    final int g = (rgb >> 8) & 0xFF;
    final int b = rgb & 0xFF;
    return r == g && g == b
      ? r
      : (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
  }

  public double getBrightness(int x, int z) {
    if (x < 0 || x >= width || z < 0 || z >= height) {
      return 0.0;
    }
    return brightnessAt(z * width + x);
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

  public double getTileRadiusGridX() {
    return tileRadiusGridX;
  }

  public double getTileRadiusGridZ() {
    return tileRadiusGridZ;
  }

  public int getTileRadiusBlocksX() {
    return tileRadiusBlocksX;
  }

  public int getTileRadiusBlocksZ() {
    return tileRadiusBlocksZ;
  }

  public static BufferedImage loadImage(String mapName) {
    String cacheKey = getProfileId() + ":" + mapName;
    synchronized (imageCache) {
      final SoftReference<BufferedImage> reference = imageCache.get(cacheKey);
      final BufferedImage cached = reference == null ? null : reference.get();
      if (cached != null) {
        return cached;
      }
    }

    String profileId = getProfileId();
    try (
      InputStream mapStream = ProfileManager.getMapStream(profileId, mapName)
    ) {
      if (mapStream == null) {
        TFCRealWorld.LOGGER.error(
          "Map {} not found for profile {} in resources",
          mapName,
          profileId
        );
        return null;
      }
      BufferedImage image = ImageIO.read(mapStream);
      if (image != null) {
        synchronized (imageCache) {
          imageCache.put(cacheKey, new SoftReference<>(image));
        }
      }
      return image;
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error(
        "Failed to load {} map for profile {} from resources",
        mapName,
        profileId,
        e
      );
      return null;
    }
  }

  private static String getProfileId() {
    return TFCRealWorldConfig.MAP_PROFILE.get();
  }

  public static void clearImageCache() {
    synchronized (imageCache) {
      imageCache.clear();
    }
  }
}
