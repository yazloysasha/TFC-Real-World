package net.yazloysasha.tfcrealworld.world.noise;

import com.mojang.logging.LogUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import net.yazloysasha.tfcrealworld.util.MapPathHelper;
import org.slf4j.Logger;

/**
 * Noise generator that reads Köppen climate classification from a PNG map.
 * The map uses indexed color with fixed colors matching KOPPEN_COLORS from maps.py.
 * Returns the Köppen climate code as a string (e.g., "AF", "BWH", etc.).
 */
public class PNGKoppenNoise {

  private static final Logger LOGGER = LogUtils.getLogger();

  // Köppen climate classification colors (from maps.py)
  private static final Map<
    Integer,
    KoppenClimateClassification
  > COLOR_TO_CLIMATE = new HashMap<>();

  static {
    COLOR_TO_CLIMATE.put(rgb(0, 0, 220), KoppenClimateClassification.AF);
    COLOR_TO_CLIMATE.put(rgb(0, 100, 240), KoppenClimateClassification.AS);
    COLOR_TO_CLIMATE.put(rgb(0, 150, 220), KoppenClimateClassification.AW);
    COLOR_TO_CLIMATE.put(rgb(40, 80, 200), KoppenClimateClassification.AM);
    COLOR_TO_CLIMATE.put(rgb(210, 0, 0), KoppenClimateClassification.BWH);
    COLOR_TO_CLIMATE.put(rgb(210, 120, 0), KoppenClimateClassification.BSH);
    COLOR_TO_CLIMATE.put(rgb(200, 80, 80), KoppenClimateClassification.BWK);
    COLOR_TO_CLIMATE.put(rgb(200, 120, 60), KoppenClimateClassification.BSK);
    COLOR_TO_CLIMATE.put(rgb(250, 250, 0), KoppenClimateClassification.CSA);
    COLOR_TO_CLIMATE.put(rgb(180, 180, 0), KoppenClimateClassification.CSB);
    COLOR_TO_CLIMATE.put(rgb(120, 120, 0), KoppenClimateClassification.CSC);
    COLOR_TO_CLIMATE.put(rgb(100, 240, 130), KoppenClimateClassification.CWA);
    COLOR_TO_CLIMATE.put(rgb(80, 210, 120), KoppenClimateClassification.CWB);
    COLOR_TO_CLIMATE.put(rgb(70, 160, 110), KoppenClimateClassification.CWC);
    COLOR_TO_CLIMATE.put(rgb(170, 240, 90), KoppenClimateClassification.CFA);
    COLOR_TO_CLIMATE.put(rgb(140, 200, 80), KoppenClimateClassification.CFB);
    COLOR_TO_CLIMATE.put(rgb(110, 170, 70), KoppenClimateClassification.CFC);
    COLOR_TO_CLIMATE.put(rgb(190, 20, 190), KoppenClimateClassification.DSA);
    COLOR_TO_CLIMATE.put(rgb(160, 20, 180), KoppenClimateClassification.DSB);
    COLOR_TO_CLIMATE.put(rgb(130, 20, 170), KoppenClimateClassification.DSC);
    COLOR_TO_CLIMATE.put(rgb(100, 20, 160), KoppenClimateClassification.DSD);
    COLOR_TO_CLIMATE.put(rgb(40, 190, 190), KoppenClimateClassification.DFA);
    COLOR_TO_CLIMATE.put(rgb(30, 170, 170), KoppenClimateClassification.DFB);
    COLOR_TO_CLIMATE.put(rgb(20, 150, 140), KoppenClimateClassification.DFC);
    COLOR_TO_CLIMATE.put(rgb(10, 130, 110), KoppenClimateClassification.DFD);
    COLOR_TO_CLIMATE.put(rgb(80, 80, 220), KoppenClimateClassification.DWA);
    COLOR_TO_CLIMATE.put(rgb(70, 70, 190), KoppenClimateClassification.DWB);
    COLOR_TO_CLIMATE.put(rgb(60, 60, 160), KoppenClimateClassification.DWC);
    COLOR_TO_CLIMATE.put(rgb(60, 60, 130), KoppenClimateClassification.DWD);
    COLOR_TO_CLIMATE.put(rgb(190, 190, 190), KoppenClimateClassification.ET);
    COLOR_TO_CLIMATE.put(rgb(80, 80, 80), KoppenClimateClassification.EF);
  }

  private static int rgb(int r, int g, int b) {
    return (r << 16) | (g << 8) | b;
  }

  private final int[] pixels;
  private final int width;
  private final int height;
  private final double centerX;
  private final double centerZ;
  private final double scaleX;
  private final double scaleZ;
  private final int worldRadiusBlocksX;
  private final int worldRadiusBlocksZ;
  private final double worldRadiusGridX;
  private final double worldRadiusGridZ;

  public PNGKoppenNoise(int horizontalWorldScale, int verticalWorldScale) {
    this.worldRadiusBlocksX = horizontalWorldScale / 2;
    this.worldRadiusBlocksZ = verticalWorldScale / 2;
    this.worldRadiusGridX =
      worldRadiusBlocksX /
      (double) net.dries007.tfc.world.region.Units.GRID_WIDTH_IN_BLOCK;
    this.worldRadiusGridZ =
      worldRadiusBlocksZ /
      (double) net.dries007.tfc.world.region.Units.GRID_WIDTH_IN_BLOCK;

    BufferedImage image = loadImage("koppen");
    if (image == null) {
      throw new RuntimeException(
        "Failed to load koppen map. Map file is required when generating climate from Köppen map."
      );
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
      "Loaded koppen map: {}x{} pixels, covering {}x{} blocks (radius X: {} blocks, Z: {} blocks)",
      width,
      height,
      worldRadiusBlocksX * 2,
      worldRadiusBlocksZ * 2,
      worldRadiusBlocksX,
      worldRadiusBlocksZ
    );
  }

  /**
   * Gets the Köppen climate classification at the given world coordinates.
   * Uses bilinear interpolation to sample from the map.
   */
  public KoppenClimateClassification getClimate(double x, double z) {
    double[] imageCoords = worldToImage(x, z);
    return sampleClimate(imageCoords[0], imageCoords[1]);
  }

  /**
   * Gets information about neighboring climates for smooth interpolation.
   * Returns the four corner climates and interpolation weights.
   */
  public ClimateInterpolationResult getClimateInterpolation(
    double x,
    double z
  ) {
    double[] imageCoords = worldToImage(x, z);
    return sampleClimateInterpolation(imageCoords[0], imageCoords[1]);
  }

  /**
   * Result of climate interpolation with four corner climates and weights.
   */
  public static class ClimateInterpolationResult {

    public final KoppenClimateClassification climate00;
    public final KoppenClimateClassification climate10;
    public final KoppenClimateClassification climate01;
    public final KoppenClimateClassification climate11;
    public final double weight00;
    public final double weight10;
    public final double weight01;
    public final double weight11;

    public ClimateInterpolationResult(
      KoppenClimateClassification climate00,
      KoppenClimateClassification climate10,
      KoppenClimateClassification climate01,
      KoppenClimateClassification climate11,
      double weight00,
      double weight10,
      double weight01,
      double weight11
    ) {
      this.climate00 = climate00;
      this.climate10 = climate10;
      this.climate01 = climate01;
      this.climate11 = climate11;
      this.weight00 = weight00;
      this.weight10 = weight10;
      this.weight01 = weight01;
      this.weight11 = weight11;
    }
  }

  private ClimateInterpolationResult sampleClimateInterpolation(
    double imageX,
    double imageZ
  ) {
    int x0 = (int) Math.floor(imageX);
    int z0 = (int) Math.floor(imageZ);
    int x1 = Math.min(x0 + 1, width - 1);
    int z1 = Math.min(z0 + 1, height - 1);

    KoppenClimateClassification climate00 = getClimateFromPixel(
      pixels[z0 * width + x0]
    );
    KoppenClimateClassification climate10 = getClimateFromPixel(
      pixels[z0 * width + x1]
    );
    KoppenClimateClassification climate01 = getClimateFromPixel(
      pixels[z1 * width + x0]
    );
    KoppenClimateClassification climate11 = getClimateFromPixel(
      pixels[z1 * width + x1]
    );
    double fx = imageX - x0;
    double fz = imageZ - z0;

    double weight00 = (1.0 - fx) * (1.0 - fz);
    double weight10 = fx * (1.0 - fz);
    double weight01 = (1.0 - fx) * fz;
    double weight11 = fx * fz;

    return new ClimateInterpolationResult(
      climate00,
      climate10,
      climate01,
      climate11,
      weight00,
      weight10,
      weight01,
      weight11
    );
  }

  private KoppenClimateClassification sampleClimate(
    double imageX,
    double imageZ
  ) {
    int x0 = (int) Math.floor(imageX);
    int z0 = (int) Math.floor(imageZ);
    int x1 = Math.min(x0 + 1, width - 1);
    int z1 = Math.min(z0 + 1, height - 1);

    KoppenClimateClassification climate00 = getClimateFromPixel(
      pixels[z0 * width + x0]
    );
    KoppenClimateClassification climate10 = getClimateFromPixel(
      pixels[z0 * width + x1]
    );
    KoppenClimateClassification climate01 = getClimateFromPixel(
      pixels[z1 * width + x0]
    );
    KoppenClimateClassification climate11 = getClimateFromPixel(
      pixels[z1 * width + x1]
    );

    if (
      climate00 == climate10 && climate00 == climate01 && climate00 == climate11
    ) {
      return climate00;
    }

    double fx = imageX - x0;
    double fz = imageZ - z0;
    if (fx < 0.5 && fz < 0.5) {
      return climate00;
    } else if (fx >= 0.5 && fz < 0.5) {
      return climate10;
    } else if (fx < 0.5 && fz >= 0.5) {
      return climate01;
    } else {
      return climate11;
    }
  }

  private KoppenClimateClassification getClimateFromPixel(int rgb) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    int rgbKey = rgb(r, g, b);

    KoppenClimateClassification climate = COLOR_TO_CLIMATE.get(rgbKey);
    if (climate != null) {
      return climate;
    }

    int minDistance = Integer.MAX_VALUE;
    KoppenClimateClassification closestClimate = KoppenClimateClassification.EF;

    for (Map.Entry<
      Integer,
      KoppenClimateClassification
    > entry : COLOR_TO_CLIMATE.entrySet()) {
      int colorKey = entry.getKey();
      int cr = (colorKey >> 16) & 0xFF;
      int cg = (colorKey >> 8) & 0xFF;
      int cb = colorKey & 0xFF;

      int distance =
        (r - cr) * (r - cr) + (g - cg) * (g - cg) + (b - cb) * (b - cb);
      if (distance < minDistance) {
        minDistance = distance;
        closestClimate = entry.getValue();
      }
    }

    return closestClimate;
  }

  private double[] worldToImage(double x, double z) {
    double clampedX = Math.clamp(x, -worldRadiusGridX, worldRadiusGridX);
    double clampedZ = Math.clamp(z, -worldRadiusGridZ, worldRadiusGridZ);

    double imageX = centerX + clampedX * scaleX;
    double imageZ = centerZ + clampedZ * scaleZ;

    imageX = Math.clamp(imageX, 0, width - 1);
    imageZ = Math.clamp(imageZ, 0, height - 1);

    return new double[] { imageX, imageZ };
  }

  private BufferedImage loadImage(String mapName) {
    Path mapPath = MapPathHelper.getMapPath(mapName);
    try {
      if (!Files.exists(mapPath)) {
        LOGGER.error("{} map not found at: {}", mapName, mapPath);
        return null;
      }
      return ImageIO.read(mapPath.toFile());
    } catch (IOException e) {
      LOGGER.error("Failed to load {} map from: {}", mapName, mapPath, e);
      return null;
    }
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }
}
