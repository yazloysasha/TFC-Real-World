package net.yazloysasha.tfcrealworld.world.noise.png;

/**
 * {@code continent.png} grayscale bands (hotspot-style discrete centers).
 * Continuous shelf / trench continuum still comes from {@link PNGAltitudeNoise}.
 *
 * <pre>
 * Band centers (uint8) — equal spacing like hotspots [0,64,127,192,255]:
 *   0   ocean
 *   85  island
 *   170 lake   (mid band — between island and white land)
 *   255 land   (white — classic binary land)
 *
 * Gaps between centers are all 85 (even). Midpoint thresholds:
 *   brightness ≤ 42.5  → OCEAN
 *   brightness ≤ 127.5 → ISLAND
 *   brightness ≤ 212.5 → LAKE
 *   brightness > 212.5 → LAND
 *
 * Classic B/W maps (0 / 255 only): black→ocean, white→land unchanged.
 * Lake/island appear only when those mid-gray bands are painted.
 * </pre>
 */
public class PNGContinentNoise extends BasePNGNoise {

  private static final String MAP_NAME = "continent";

  /** Painted ocean center. */
  public static final int BAND_OCEAN = 0;

  /** Painted island center. */
  public static final int BAND_ISLAND = 85;

  /** Painted lake center (mid gray; not used by classic B/W maps). */
  public static final int BAND_LAKE = 170;

  /** Painted land center (white; classic binary land). */
  public static final int BAND_LAND = 255;

  /**
   * Midpoint ocean↔island. {@code brightness ≤ this} → ocean.
   */
  public static final double THRESHOLD_OCEAN_MAX = 42.5;

  /**
   * Midpoint island↔lake. {@code brightness ≤ this} (and &gt; ocean max) → island.
   */
  public static final double THRESHOLD_ISLAND_MAX = 127.5;

  /**
   * Midpoint lake↔land. {@code brightness ≤ this} (and &gt; island max) → lake;
   * above → land. Classic white (255) stays land.
   */
  public static final double THRESHOLD_LAKE_MAX = 212.5;

  /**
   * Legacy name: values above this were "land" on binary maps. Kept as
   * documentation alias for the pre-band land/ocean cut (~127). Prefer
   * {@link #bandFromBrightness} / {@link #THRESHOLD_LAKE_MAX} for band logic.
   * Non-ocean membership is {@code band != OCEAN} (island/lake/land).
   */
  public static final int LAND_MASK_THRESHOLD = 127;

  /** Hard land sample in the 0–10 continent-noise scale. */
  public static final double LAND_NOISE = 10.0;

  /** Hard ocean sample in the 0–10 continent-noise scale. */
  public static final double OCEAN_NOISE = 0.0;

  public enum ContinentBand {
    OCEAN,
    ISLAND,
    LAKE,
    LAND,
  }

  public PNGContinentNoise(int horizontalScale, int verticalScale) {
    super(
      horizontalScale,
      verticalScale,
      MAP_NAME,
      "Failed to load continent map. Map file is required when generating continents from map."
    );
  }

  /**
   * Classify a grayscale sample into a continent band (hotspot-style midpoints).
   */
  public static ContinentBand bandFromBrightness(double brightness) {
    if (brightness <= THRESHOLD_OCEAN_MAX) {
      return ContinentBand.OCEAN;
    }
    if (brightness <= THRESHOLD_ISLAND_MAX) {
      return ContinentBand.ISLAND;
    }
    if (brightness <= THRESHOLD_LAKE_MAX) {
      return ContinentBand.LAKE;
    }
    return ContinentBand.LAND;
  }

  @Override
  protected double transformBrightness(double brightness) {
    return bandFromBrightness(brightness) == ContinentBand.OCEAN
      ? OCEAN_NOISE
      : LAND_NOISE;
  }

  public ContinentBand bandAtGridHard(double gridX, double gridZ) {
    return bandFromBrightness(sampleGrayAtWorldRounded(gridX, gridZ));
  }

  public ContinentBand bandAtPixel(int x, int z) {
    return bandFromBrightness(getBrightness(x, z));
  }

  /**
   * Non-ocean sample (island / lake / land). Used by distance caches and biome
   * land/ocean correction. Classic white land and new mid bands all count.
   */
  public boolean isLandAtGridHard(double gridX, double gridZ) {
    return bandAtGridHard(gridX, gridZ) != ContinentBand.OCEAN;
  }

  /**
   * Pixel-space non-ocean test (same bands as {@link #isLandAtGridHard}).
   */
  public boolean isLandPixel(int x, int z) {
    return bandAtPixel(x, z) != ContinentBand.OCEAN;
  }

  public boolean isOceanAtGridHard(double gridX, double gridZ) {
    return bandAtGridHard(gridX, gridZ) == ContinentBand.OCEAN;
  }

  public boolean isLakeAtGridHard(double gridX, double gridZ) {
    return bandAtGridHard(gridX, gridZ) == ContinentBand.LAKE;
  }

  public boolean isIslandAtGridHard(double gridX, double gridZ) {
    return bandAtGridHard(gridX, gridZ) == ContinentBand.ISLAND;
  }
}
