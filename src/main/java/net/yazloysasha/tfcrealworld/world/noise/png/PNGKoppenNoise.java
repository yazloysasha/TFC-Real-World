package net.yazloysasha.tfcrealworld.world.noise.png;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.world.climate.RealKoppenClimateClassification;

public class PNGKoppenNoise {

  private static final Map<
    Integer,
    RealKoppenClimateClassification
  > COLOR_TO_CLIMATE = new HashMap<>();

  static {
    COLOR_TO_CLIMATE.put(rgb(0, 0, 220), RealKoppenClimateClassification.AF);
    COLOR_TO_CLIMATE.put(rgb(0, 100, 240), RealKoppenClimateClassification.AS);
    COLOR_TO_CLIMATE.put(rgb(0, 150, 220), RealKoppenClimateClassification.AW);
    COLOR_TO_CLIMATE.put(rgb(40, 80, 200), RealKoppenClimateClassification.AM);
    COLOR_TO_CLIMATE.put(rgb(210, 0, 0), RealKoppenClimateClassification.BWH);
    COLOR_TO_CLIMATE.put(rgb(210, 120, 0), RealKoppenClimateClassification.BSH);
    COLOR_TO_CLIMATE.put(rgb(200, 80, 80), RealKoppenClimateClassification.BWK);
    COLOR_TO_CLIMATE.put(
      rgb(200, 120, 60),
      RealKoppenClimateClassification.BSK
    );
    COLOR_TO_CLIMATE.put(rgb(250, 250, 0), RealKoppenClimateClassification.CSA);
    COLOR_TO_CLIMATE.put(rgb(180, 180, 0), RealKoppenClimateClassification.CSB);
    COLOR_TO_CLIMATE.put(rgb(120, 120, 0), RealKoppenClimateClassification.CSC);
    COLOR_TO_CLIMATE.put(
      rgb(100, 240, 130),
      RealKoppenClimateClassification.CWA
    );
    COLOR_TO_CLIMATE.put(
      rgb(80, 210, 120),
      RealKoppenClimateClassification.CWB
    );
    COLOR_TO_CLIMATE.put(
      rgb(70, 160, 110),
      RealKoppenClimateClassification.CWC
    );
    COLOR_TO_CLIMATE.put(
      rgb(170, 240, 90),
      RealKoppenClimateClassification.CFA
    );
    COLOR_TO_CLIMATE.put(
      rgb(140, 200, 80),
      RealKoppenClimateClassification.CFB
    );
    COLOR_TO_CLIMATE.put(
      rgb(110, 170, 70),
      RealKoppenClimateClassification.CFC
    );
    COLOR_TO_CLIMATE.put(
      rgb(190, 20, 190),
      RealKoppenClimateClassification.DSA
    );
    COLOR_TO_CLIMATE.put(
      rgb(160, 20, 180),
      RealKoppenClimateClassification.DSB
    );
    COLOR_TO_CLIMATE.put(
      rgb(130, 20, 170),
      RealKoppenClimateClassification.DSC
    );
    COLOR_TO_CLIMATE.put(
      rgb(100, 20, 160),
      RealKoppenClimateClassification.DSD
    );
    COLOR_TO_CLIMATE.put(
      rgb(40, 190, 190),
      RealKoppenClimateClassification.DFA
    );
    COLOR_TO_CLIMATE.put(
      rgb(30, 170, 170),
      RealKoppenClimateClassification.DFB
    );
    COLOR_TO_CLIMATE.put(
      rgb(20, 150, 140),
      RealKoppenClimateClassification.DFC
    );
    COLOR_TO_CLIMATE.put(
      rgb(10, 130, 110),
      RealKoppenClimateClassification.DFD
    );
    COLOR_TO_CLIMATE.put(rgb(80, 80, 220), RealKoppenClimateClassification.DWA);
    COLOR_TO_CLIMATE.put(rgb(70, 70, 190), RealKoppenClimateClassification.DWB);
    COLOR_TO_CLIMATE.put(rgb(60, 60, 160), RealKoppenClimateClassification.DWC);
    COLOR_TO_CLIMATE.put(rgb(60, 60, 130), RealKoppenClimateClassification.DWD);
    COLOR_TO_CLIMATE.put(
      rgb(190, 190, 190),
      RealKoppenClimateClassification.ET
    );
    COLOR_TO_CLIMATE.put(rgb(80, 80, 80), RealKoppenClimateClassification.EF);
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
  private final int tileRadiusBlocksX;
  private final int tileRadiusBlocksZ;
  private final double tileRadiusGridX;
  private final double tileRadiusGridZ;

  public PNGKoppenNoise(int horizontalScale, int verticalScale) {
    this.tileRadiusBlocksX = horizontalScale / 2;
    this.tileRadiusBlocksZ = verticalScale / 2;
    this.tileRadiusGridX =
      tileRadiusBlocksX / (double) TFCRealWorld.GRID_WIDTH_IN_BLOCK;
    this.tileRadiusGridZ =
      tileRadiusBlocksZ / (double) TFCRealWorld.GRID_WIDTH_IN_BLOCK;

    BufferedImage image = BasePNGNoise.loadImage("koppen");
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

    this.scaleX = width / (2.0 * tileRadiusGridX);
    this.scaleZ = height / (2.0 * tileRadiusGridZ);
  }

  public RealKoppenClimateClassification getClimate(double x, double z) {
    double[] imageCoords = tileToImage(x, z);
    return sampleClimate(imageCoords[0], imageCoords[1]);
  }

  public ClimateInterpolationResult getClimateInterpolation(
    double x,
    double z
  ) {
    double[] imageCoords = tileToImage(x, z);
    return sampleClimateInterpolation(imageCoords[0], imageCoords[1]);
  }

  public static class ClimateInterpolationResult {

    public final RealKoppenClimateClassification climate00;
    public final RealKoppenClimateClassification climate10;
    public final RealKoppenClimateClassification climate01;
    public final RealKoppenClimateClassification climate11;
    public final double weight00;
    public final double weight10;
    public final double weight01;
    public final double weight11;

    public ClimateInterpolationResult(
      RealKoppenClimateClassification climate00,
      RealKoppenClimateClassification climate10,
      RealKoppenClimateClassification climate01,
      RealKoppenClimateClassification climate11,
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

    RealKoppenClimateClassification climate00 = getClimateFromPixel(
      pixels[z0 * width + x0]
    );
    RealKoppenClimateClassification climate10 = getClimateFromPixel(
      pixels[z0 * width + x1]
    );
    RealKoppenClimateClassification climate01 = getClimateFromPixel(
      pixels[z1 * width + x0]
    );
    RealKoppenClimateClassification climate11 = getClimateFromPixel(
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

  private RealKoppenClimateClassification sampleClimate(
    double imageX,
    double imageZ
  ) {
    int x0 = (int) Math.floor(imageX);
    int z0 = (int) Math.floor(imageZ);
    int x1 = Math.min(x0 + 1, width - 1);
    int z1 = Math.min(z0 + 1, height - 1);

    RealKoppenClimateClassification climate00 = getClimateFromPixel(
      pixels[z0 * width + x0]
    );
    RealKoppenClimateClassification climate10 = getClimateFromPixel(
      pixels[z0 * width + x1]
    );
    RealKoppenClimateClassification climate01 = getClimateFromPixel(
      pixels[z1 * width + x0]
    );
    RealKoppenClimateClassification climate11 = getClimateFromPixel(
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

  public static RealKoppenClimateClassification getClimateFromRgb(int rgb) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    int rgbKey = rgb(r, g, b);

    RealKoppenClimateClassification climate = COLOR_TO_CLIMATE.get(rgbKey);
    if (climate != null) {
      return climate;
    }

    int minDistance = Integer.MAX_VALUE;
    RealKoppenClimateClassification closestClimate =
      RealKoppenClimateClassification.EF;

    for (Map.Entry<
      Integer,
      RealKoppenClimateClassification
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

  private RealKoppenClimateClassification getClimateFromPixel(int rgb) {
    return getClimateFromRgb(rgb);
  }

  private double[] tileToImage(double x, double z) {
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

    double clampedX = Mth.clamp(localX, -tileRadiusGridX, tileRadiusGridX);
    double clampedZ = Mth.clamp(localZ, -tileRadiusGridZ, tileRadiusGridZ);

    double imageX = centerX + clampedX * scaleX;
    double imageZ = centerZ + clampedZ * scaleZ;

    imageX = Mth.clamp(imageX, 0, width - 1);
    imageZ = Mth.clamp(imageZ, 0, height - 1);

    return new double[] { imageX, imageZ };
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }
}
