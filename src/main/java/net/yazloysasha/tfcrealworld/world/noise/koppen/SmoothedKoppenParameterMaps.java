package net.yazloysasha.tfcrealworld.world.noise.koppen;

import java.awt.image.BufferedImage;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.noise.png.BasePNGNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;

/**
 * Builds per-pixel (map-grid) temperature/rainfall/rainVar fields based on the Köppen map,
 * then enforces local Lipschitz constraints so that no neighboring cells differ by more than:
 * - 1.0°C temperature
 * - 10mm rainfall
 * - 0.1 rainVar
 *
 * Sampling is bilinear in image space, so once the grid-level constraints are satisfied,
 * the continuous field will not re-introduce sharp 1-cell transitions.
 */
public final class SmoothedKoppenParameterMaps {

  private static final float MAX_DT = 1.0f;
  private static final float MAX_DR = 10.0f;
  private static final float MAX_DV = 0.1f;

  private static final float TEMP_MIN = -20.0f;
  private static final float TEMP_MAX = 30.0f;
  private static final float RAIN_MIN = 0.0f;
  private static final float RAIN_MAX = 500.0f;
  private static final float RAINVAR_MIN = -1.0f;
  private static final float RAINVAR_MAX = 1.0f;

  // Conservative cap: in practice it converges much faster, and we break early once stable.
  private static final int MAX_SMOOTHING_ITERATIONS = 256;

  private static volatile SmoothedKoppenParameterMaps instance;

  private final String profileId;
  private final int width;
  private final int height;

  private final float[] temperatures;
  private final float[] rainfalls;
  private final float[] rainVars;

  private SmoothedKoppenParameterMaps(
    String profileId,
    int width,
    int height,
    float[] temperatures,
    float[] rainfalls,
    float[] rainVars
  ) {
    this.profileId = profileId;
    this.width = width;
    this.height = height;
    this.temperatures = temperatures;
    this.rainfalls = rainfalls;
    this.rainVars = rainVars;
  }

  public static SmoothedKoppenParameterMaps getInstance() {
    String profileId = TFCRealWorldConfig.MAP_PROFILE.get();

    SmoothedKoppenParameterMaps cached = instance;
    if (cached != null && cached.profileId.equals(profileId)) {
      return cached;
    }

    synchronized (SmoothedKoppenParameterMaps.class) {
      cached = instance;
      if (cached != null && cached.profileId.equals(profileId)) {
        return cached;
      }

      instance = build(profileId);
      return instance;
    }
  }

  public static void clear() {
    instance = null;
  }

  public double sampleTemperature(double imageX, double imageZ) {
    return sampleBilinear(temperatures, imageX, imageZ);
  }

  public double sampleRainfall(double imageX, double imageZ) {
    return sampleBilinear(rainfalls, imageX, imageZ);
  }

  public double sampleRainVar(double imageX, double imageZ) {
    return sampleBilinear(rainVars, imageX, imageZ);
  }

  private static SmoothedKoppenParameterMaps build(String profileId) {
    BufferedImage koppenImage = BasePNGNoise.loadImage("koppen");
    BufferedImage temperatureImage = BasePNGNoise.loadImage("temperature");
    BufferedImage rainfallImage = BasePNGNoise.loadImage("rainfall");

    if (
      koppenImage == null || temperatureImage == null || rainfallImage == null
    ) {
      throw new RuntimeException(
        "Failed to load required maps (koppen/temperature/rainfall) for profile " +
        profileId
      );
    }

    final int width = koppenImage.getWidth();
    final int height = koppenImage.getHeight();

    if (
      temperatureImage.getWidth() != width ||
      temperatureImage.getHeight() != height ||
      rainfallImage.getWidth() != width ||
      rainfallImage.getHeight() != height
    ) {
      throw new RuntimeException(
        "Map size mismatch for profile " +
        profileId +
        ": koppen=" +
        width +
        "x" +
        height +
        ", temperature=" +
        temperatureImage.getWidth() +
        "x" +
        temperatureImage.getHeight() +
        ", rainfall=" +
        rainfallImage.getWidth() +
        "x" +
        rainfallImage.getHeight()
      );
    }

    final int size = width * height;
    int[] koppenPixels = new int[size];
    int[] tempPixels = new int[size];
    int[] rainPixels = new int[size];

    koppenImage.getRGB(0, 0, width, height, koppenPixels, 0, width);
    temperatureImage.getRGB(0, 0, width, height, tempPixels, 0, width);
    rainfallImage.getRGB(0, 0, width, height, rainPixels, 0, width);

    float[] temperatures = new float[size];
    float[] rainfalls = new float[size];
    float[] rainVars = new float[size];

    KoppenParameterCache cache = KoppenParameterCache.getInstance();

    for (int i = 0; i < size; i++) {
      KoppenClimateClassification climate = PNGKoppenNoise.getClimateFromRgb(
        koppenPixels[i]
      );
      double tempGray = brightness(tempPixels[i]);
      double rainGray = brightness(rainPixels[i]);

      KoppenParameterCache.ParameterCombination params =
        cache.getParametersFromGrayscale(climate, tempGray, rainGray);

      temperatures[i] = params.temperature;
      rainfalls[i] = params.rainfall;
      rainVars[i] = params.rainVar;
    }

    long startNs = System.nanoTime();
    int iterations = enforceNeighborLimits(
      temperatures,
      rainfalls,
      rainVars,
      width,
      height,
      MAX_SMOOTHING_ITERATIONS
    );
    long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

    TFCRealWorld.LOGGER.info(
      "Built smoothed Köppen parameter maps for profile {} ({}x{}), iterations={}, took={}ms",
      profileId,
      width,
      height,
      iterations,
      elapsedMs
    );

    return new SmoothedKoppenParameterMaps(
      profileId,
      width,
      height,
      temperatures,
      rainfalls,
      rainVars
    );
  }

  static int enforceNeighborLimits(
    float[] temperatures,
    float[] rainfalls,
    float[] rainVars,
    int width,
    int height,
    int maxIterations
  ) {
    int performedIterations = 0;

    for (int iter = 0; iter < maxIterations; iter++) {
      boolean changed = false;

      // Horizontal edges
      for (int z = 0; z < height; z++) {
        int rowStart = z * width;
        for (int x = 0; x < width - 1; x++) {
          int i = rowStart + x;
          int j = i + 1;
          changed |= relaxPair(temperatures, i, j, MAX_DT, TEMP_MIN, TEMP_MAX);
          changed |= relaxPair(rainfalls, i, j, MAX_DR, RAIN_MIN, RAIN_MAX);
          changed |= relaxPair(
            rainVars,
            i,
            j,
            MAX_DV,
            RAINVAR_MIN,
            RAINVAR_MAX
          );
        }
      }

      // Vertical edges
      for (int z = 0; z < height - 1; z++) {
        int rowStart = z * width;
        int nextRowStart = (z + 1) * width;
        for (int x = 0; x < width; x++) {
          int i = rowStart + x;
          int j = nextRowStart + x;
          changed |= relaxPair(temperatures, i, j, MAX_DT, TEMP_MIN, TEMP_MAX);
          changed |= relaxPair(rainfalls, i, j, MAX_DR, RAIN_MIN, RAIN_MAX);
          changed |= relaxPair(
            rainVars,
            i,
            j,
            MAX_DV,
            RAINVAR_MIN,
            RAINVAR_MAX
          );
        }
      }

      performedIterations = iter + 1;
      if (!changed) {
        break;
      }
    }

    return performedIterations;
  }

  private static boolean relaxPair(
    float[] values,
    int i,
    int j,
    float maxDelta,
    float min,
    float max
  ) {
    float a = values[i];
    float b = values[j];
    float diff = b - a;
    float abs = Math.abs(diff);
    if (abs <= maxDelta) {
      return false;
    }

    float excess = abs - maxDelta;
    float move = excess * 0.5f;

    if (diff > 0) {
      a += move;
      b -= move;
    } else {
      a -= move;
      b += move;
    }

    values[i] = Mth.clamp(a, min, max);
    values[j] = Mth.clamp(b, min, max);
    return true;
  }

  private double sampleBilinear(float[] values, double imageX, double imageZ) {
    // tileToImage already clamps, but keep it safe.
    imageX = Math.clamp(imageX, 0, width - 1);
    imageZ = Math.clamp(imageZ, 0, height - 1);

    int x0 = (int) Math.floor(imageX);
    int z0 = (int) Math.floor(imageZ);
    int x1 = Math.min(x0 + 1, width - 1);
    int z1 = Math.min(z0 + 1, height - 1);

    double fx = imageX - x0;
    double fz = imageZ - z0;

    float v00 = values[z0 * width + x0];
    float v10 = values[z0 * width + x1];
    float v01 = values[z1 * width + x0];
    float v11 = values[z1 * width + x1];

    double v0 = v00 * (1.0 - fx) + v10 * fx;
    double v1 = v01 * (1.0 - fx) + v11 * fx;
    return v0 * (1.0 - fz) + v1 * fz;
  }

  private static double brightness(int rgb) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    return 0.299 * r + 0.587 * g + 0.114 * b;
  }
}
