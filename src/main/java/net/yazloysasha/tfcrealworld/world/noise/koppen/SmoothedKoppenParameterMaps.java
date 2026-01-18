package net.yazloysasha.tfcrealworld.world.noise.koppen;

import java.awt.image.BufferedImage;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.noise.png.BasePNGNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;

/**
 * Smoothed parameter maps (temperature, rainfall, rainVar) derived from the Köppen map.
 * Enforces local neighbor limits in map-grid space.
 */
public final class SmoothedKoppenParameterMaps {

  private static final float TEMP_MIN = -20.0f;
  private static final float TEMP_MAX = 30.0f;
  private static final float RAIN_MAX = 500.0f;
  private static final float RAINVAR_MIN = -1.0f;
  private static final float RAINVAR_MAX = 1.0f;

  // Stored as discrete indices (1°C / 10mm / 0.1) and enforced in index space.
  private static final int TEMP_IDX_MIN = 0;
  private static final int TEMP_IDX_MAX = (int) (TEMP_MAX - TEMP_MIN); // 50
  private static final int RAIN_IDX_MIN = 0;
  private static final int RAIN_IDX_MAX = (int) (RAIN_MAX / 10.0f); // 50
  private static final int RAINVAR_IDX_MIN = 0;
  private static final int RAINVAR_IDX_MAX = (int) ((RAINVAR_MAX -
      RAINVAR_MIN) /
    0.1f); // 20

  // Bit-packed indices per pixel.
  private static final int TEMP_BITS = 6;
  private static final int RAIN_BITS = 6;
  private static final int RAINVAR_BITS = 5;

  private static final int TEMP_SHIFT = 0;
  private static final int RAIN_SHIFT = TEMP_SHIFT + TEMP_BITS; // 6
  private static final int RAINVAR_SHIFT = RAIN_SHIFT + RAIN_BITS; // 12

  private static final int TEMP_MASK = (1 << TEMP_BITS) - 1; // 0x3F
  private static final int RAIN_MASK = (1 << RAIN_BITS) - 1; // 0x3F
  private static final int RAINVAR_MASK = (1 << RAINVAR_BITS) - 1; // 0x1F

  // Safety caps; fallback full scan guarantees convergence.
  private static final int MAX_SMOOTHING_DEQUEUES_MULTIPLIER = 64;
  private static final int MAX_FALLBACK_FULL_SCAN_ITERATIONS = 256;

  private static volatile SmoothedKoppenParameterMaps instance;

  private final String profileId;
  private final int width;
  private final int height;

  private final int[] packedIdx;

  private SmoothedKoppenParameterMaps(
    String profileId,
    int width,
    int height,
    int[] packedIdx
  ) {
    this.profileId = profileId;
    this.width = width;
    this.height = height;
    this.packedIdx = packedIdx;
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
    double idx = sampleBilinearIndex(
      packedIdx,
      imageX,
      imageZ,
      TEMP_SHIFT,
      TEMP_MASK
    );
    return TEMP_MIN + idx;
  }

  public double sampleRainfall(double imageX, double imageZ) {
    double idx = sampleBilinearIndex(
      packedIdx,
      imageX,
      imageZ,
      RAIN_SHIFT,
      RAIN_MASK
    );
    return idx * 10.0;
  }

  public double sampleRainVar(double imageX, double imageZ) {
    double idx = sampleBilinearIndex(
      packedIdx,
      imageX,
      imageZ,
      RAINVAR_SHIFT,
      RAINVAR_MASK
    );
    return RAINVAR_MIN + idx * 0.1;
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

    int[] packedIdx = new int[size];

    KoppenParameterCache cache = KoppenParameterCache.getInstance();

    for (int i = 0; i < size; i++) {
      KoppenClimateClassification climate = PNGKoppenNoise.getClimateFromRgb(
        koppenPixels[i]
      );
      double tempGray = brightness(tempPixels[i]);
      double rainGray = brightness(rainPixels[i]);

      KoppenParameterCache.ParameterCombination params =
        cache.getParametersFromGrayscale(climate, tempGray, rainGray);

      int ti = Math.round(params.temperature - TEMP_MIN);
      int ri = Math.round(params.rainfall / 10.0f);
      int vi = Math.round((params.rainVar - RAINVAR_MIN) / 0.1f);

      ti = Mth.clamp(ti, TEMP_IDX_MIN, TEMP_IDX_MAX);
      ri = Mth.clamp(ri, RAIN_IDX_MIN, RAIN_IDX_MAX);
      vi = Mth.clamp(vi, RAINVAR_IDX_MIN, RAINVAR_IDX_MAX);

      packedIdx[i] = pack(ti, ri, vi);
    }

    long startNs = System.nanoTime();
    SmoothingStats stats = enforceNeighborLimits(packedIdx, width, height);
    long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

    TFCRealWorld.LOGGER.info(
      "Built smoothed Köppen parameter maps for profile {} ({}x{}), seededEdges={}, dequeues={}, maxQueue={}, fallbackIterations={}, took={}ms",
      profileId,
      width,
      height,
      stats.seededEdges,
      stats.dequeues,
      stats.maxQueueSize,
      stats.fallbackFullScanIterations,
      elapsedMs
    );

    return new SmoothedKoppenParameterMaps(profileId, width, height, packedIdx);
  }

  private static final class SmoothingStats {

    int seededEdges;
    int dequeues;
    int maxQueueSize;
    int fallbackFullScanIterations;
  }

  static SmoothingStats enforceNeighborLimits(
    int[] packedIdx,
    int width,
    int height
  ) {
    SmoothingStats stats = new SmoothingStats();

    final int size = width * height;
    if (size <= 1) {
      return stats;
    }

    boolean[] inQueue = new boolean[size];
    int[] queue = new int[size]; // ring buffer, no duplicates => capacity == size is enough
    int head = 0;
    int tail = 0;
    int count = 0;

    // Seed queue with violating edge endpoints.
    for (int z = 0; z < height; z++) {
      int rowStart = z * width;
      for (int x = 0; x < width - 1; x++) {
        int i = rowStart + x;
        int j = i + 1;
        if (violatesAny(packedIdx, i, j)) {
          stats.seededEdges++;
          if (!inQueue[i]) {
            inQueue[i] = true;
            queue[tail] = i;
            tail = (tail + 1) % size;
            count++;
          }
          if (!inQueue[j]) {
            inQueue[j] = true;
            queue[tail] = j;
            tail = (tail + 1) % size;
            count++;
          }
        }
      }
    }
    for (int z = 0; z < height - 1; z++) {
      int rowStart = z * width;
      int nextRowStart = (z + 1) * width;
      for (int x = 0; x < width; x++) {
        int i = rowStart + x;
        int j = nextRowStart + x;
        if (violatesAny(packedIdx, i, j)) {
          stats.seededEdges++;
          if (!inQueue[i]) {
            inQueue[i] = true;
            queue[tail] = i;
            tail = (tail + 1) % size;
            count++;
          }
          if (!inQueue[j]) {
            inQueue[j] = true;
            queue[tail] = j;
            tail = (tail + 1) % size;
            count++;
          }
        }
      }
    }

    stats.maxQueueSize = Math.max(stats.maxQueueSize, count);

    // Process only the neighborhood around violations.
    final int maxDequeues = size * MAX_SMOOTHING_DEQUEUES_MULTIPLIER;
    while (count > 0 && stats.dequeues < maxDequeues) {
      int idx = queue[head];
      head = (head + 1) % size;
      count--;
      inQueue[idx] = false;
      stats.dequeues++;

      int x = idx % width;
      int z = idx / width;

      // left
      if (x > 0) {
        int j = idx - 1;
        if (relaxAll(packedIdx, idx, j)) {
          if (!inQueue[idx]) {
            inQueue[idx] = true;
            queue[tail] = idx;
            tail = (tail + 1) % size;
            count++;
          }
          if (!inQueue[j]) {
            inQueue[j] = true;
            queue[tail] = j;
            tail = (tail + 1) % size;
            count++;
          }
        }
      }
      // right
      if (x < width - 1) {
        int j = idx + 1;
        if (relaxAll(packedIdx, idx, j)) {
          if (!inQueue[idx]) {
            inQueue[idx] = true;
            queue[tail] = idx;
            tail = (tail + 1) % size;
            count++;
          }
          if (!inQueue[j]) {
            inQueue[j] = true;
            queue[tail] = j;
            tail = (tail + 1) % size;
            count++;
          }
        }
      }
      // up
      if (z > 0) {
        int j = idx - width;
        if (relaxAll(packedIdx, idx, j)) {
          if (!inQueue[idx]) {
            inQueue[idx] = true;
            queue[tail] = idx;
            tail = (tail + 1) % size;
            count++;
          }
          if (!inQueue[j]) {
            inQueue[j] = true;
            queue[tail] = j;
            tail = (tail + 1) % size;
            count++;
          }
        }
      }
      // down
      if (z < height - 1) {
        int j = idx + width;
        if (relaxAll(packedIdx, idx, j)) {
          if (!inQueue[idx]) {
            inQueue[idx] = true;
            queue[tail] = idx;
            tail = (tail + 1) % size;
            count++;
          }
          if (!inQueue[j]) {
            inQueue[j] = true;
            queue[tail] = j;
            tail = (tail + 1) % size;
            count++;
          }
        }
      }

      if (count > stats.maxQueueSize) {
        stats.maxQueueSize = count;
      }
    }

    int remainingViolations = countViolatingEdges(packedIdx, width, height);
    if (remainingViolations > 0) {
      TFCRealWorld.LOGGER.warn(
        "Smoothing queue stopped with {} violating edges remaining (dequeues={}, maxDequeues={}). Falling back to full scans.",
        remainingViolations,
        stats.dequeues,
        maxDequeues
      );
      stats.fallbackFullScanIterations = enforceNeighborLimitsFullScans(
        packedIdx,
        width,
        height,
        MAX_FALLBACK_FULL_SCAN_ITERATIONS
      );
    }

    return stats;
  }

  private static boolean violatesAny(int[] packedIdx, int i, int j) {
    int pi = packedIdx[i];
    int pj = packedIdx[j];
    int ti = unpackTemp(pi);
    int tj = unpackTemp(pj);
    int ri = unpackRain(pi);
    int rj = unpackRain(pj);
    int vi = unpackRainVar(pi);
    int vj = unpackRainVar(pj);
    return (
      Math.abs(ti - tj) > 1 || Math.abs(ri - rj) > 1 || Math.abs(vi - vj) > 1
    );
  }

  private static boolean relaxAll(int[] packedIdx, int i, int j) {
    int pi = packedIdx[i];
    int pj = packedIdx[j];

    int ti = unpackTemp(pi);
    int tj = unpackTemp(pj);
    int ri = unpackRain(pi);
    int rj = unpackRain(pj);
    int vi = unpackRainVar(pi);
    int vj = unpackRainVar(pj);

    boolean changed = false;

    int[] t = relaxIdxPair(ti, tj, TEMP_IDX_MIN, TEMP_IDX_MAX);
    if (t != null) {
      changed = true;
      ti = t[0];
      tj = t[1];
    }

    int[] r = relaxIdxPair(ri, rj, RAIN_IDX_MIN, RAIN_IDX_MAX);
    if (r != null) {
      changed = true;
      ri = r[0];
      rj = r[1];
    }

    int[] v = relaxIdxPair(vi, vj, RAINVAR_IDX_MIN, RAINVAR_IDX_MAX);
    if (v != null) {
      changed = true;
      vi = v[0];
      vj = v[1];
    }

    if (!changed) {
      return false;
    }

    packedIdx[i] = pack(ti, ri, vi);
    packedIdx[j] = pack(tj, rj, vj);
    return true;
  }

  private static int enforceNeighborLimitsFullScans(
    int[] packedIdx,
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
          changed |= relaxAll(packedIdx, i, j);
        }
      }

      // Vertical edges
      for (int z = 0; z < height - 1; z++) {
        int rowStart = z * width;
        int nextRowStart = (z + 1) * width;
        for (int x = 0; x < width; x++) {
          int i = rowStart + x;
          int j = nextRowStart + x;
          changed |= relaxAll(packedIdx, i, j);
        }
      }

      performedIterations = iter + 1;
      if (!changed) {
        break;
      }
    }
    return performedIterations;
  }

  private static int countViolatingEdges(
    int[] packedIdx,
    int width,
    int height
  ) {
    int violations = 0;
    // Horizontal edges
    for (int z = 0; z < height; z++) {
      int rowStart = z * width;
      for (int x = 0; x < width - 1; x++) {
        int i = rowStart + x;
        int j = i + 1;
        if (violatesAny(packedIdx, i, j)) {
          violations++;
        }
      }
    }
    // Vertical edges
    for (int z = 0; z < height - 1; z++) {
      int rowStart = z * width;
      int nextRowStart = (z + 1) * width;
      for (int x = 0; x < width; x++) {
        int i = rowStart + x;
        int j = nextRowStart + x;
        if (violatesAny(packedIdx, i, j)) {
          violations++;
        }
      }
    }
    return violations;
  }

  private static int[] relaxIdxPair(int a, int b, int min, int max) {
    int diff = b - a;
    int abs = Math.abs(diff);
    if (abs <= 1) {
      return null;
    }

    int excess = abs - 1;
    int move = excess / 2;
    int remainder = excess - move * 2; // 0 or 1

    if (diff > 0) {
      a += move + remainder;
      b -= move;
    } else {
      a -= move;
      b += move + remainder;
    }

    a = Mth.clamp(a, min, max);
    b = Mth.clamp(b, min, max);
    return new int[] { a, b };
  }

  private double sampleBilinearIndex(
    int[] values,
    double imageX,
    double imageZ,
    int shift,
    int mask
  ) {
    // tileToImage already clamps, but keep it safe.
    imageX = clamp(imageX, 0.0, width - 1.0);
    imageZ = clamp(imageZ, 0.0, height - 1.0);

    int x0 = (int) Math.floor(imageX);
    int z0 = (int) Math.floor(imageZ);
    int x1 = Math.min(x0 + 1, width - 1);
    int z1 = Math.min(z0 + 1, height - 1);

    double fx = imageX - x0;
    double fz = imageZ - z0;

    double v00 = unpack(values[z0 * width + x0], shift, mask);
    double v10 = unpack(values[z0 * width + x1], shift, mask);
    double v01 = unpack(values[z1 * width + x0], shift, mask);
    double v11 = unpack(values[z1 * width + x1], shift, mask);

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

  private static int pack(int tempIdx, int rainIdx, int rainVarIdx) {
    return (
      ((tempIdx & TEMP_MASK) << TEMP_SHIFT) |
      ((rainIdx & RAIN_MASK) << RAIN_SHIFT) |
      ((rainVarIdx & RAINVAR_MASK) << RAINVAR_SHIFT)
    );
  }

  private static int unpack(int packed, int shift, int mask) {
    return (packed >> shift) & mask;
  }

  private static int unpackTemp(int packed) {
    return unpack(packed, TEMP_SHIFT, TEMP_MASK);
  }

  private static int unpackRain(int packed) {
    return unpack(packed, RAIN_SHIFT, RAIN_MASK);
  }

  private static int unpackRainVar(int packed) {
    return unpack(packed, RAINVAR_SHIFT, RAINVAR_MASK);
  }

  private static double clamp(double value, double min, double max) {
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }
}
