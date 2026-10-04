package net.yazloysasha.tfcrealworld.world.noise.png;

import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

/**
 * {@code continent.png} grayscale bands (hotspot-style discrete centers).
 * Ocean depth and relief come from {@code tectonics.png}, not from this map.
 *
 * <pre>
 * Band centers (uint8), step 64, land clamped to white:
 *   0   ocean
 *   64  island
 *   128 fresh lake
 *   192 salt lake
 *   255 land   (white — classic binary land)
 *
 * Gaps are 64, 64, 64, 63. Midpoint thresholds:
 *   brightness ≤ 32    → OCEAN
 *   brightness ≤ 96    → ISLAND
 *   brightness ≤ 160   → LAKE (fresh water)
 *   brightness ≤ 223.5 → SALT_LAKE (salt water)
 *   brightness &gt; 223.5 → LAND
 *
 * Classic B/W maps (0 / 255 only): black→ocean, white→land unchanged.
 * Island and lake bands appear only when those grays are painted.
 * </pre>
 */
public class PNGContinentNoise extends BasePNGNoise {

  private static final String MAP_NAME = "continent";

  /** Painted ocean center. */
  public static final int BAND_OCEAN = 0;

  /** Painted island center. */
  public static final int BAND_ISLAND = 64;

  /** Painted fresh-lake center. */
  public static final int BAND_LAKE = 128;

  /** Painted salt-lake center. */
  public static final int BAND_SALT_LAKE = 192;

  /** Painted land center (white; classic binary land). */
  public static final int BAND_LAND = 255;

  /** Midpoint ocean↔island. {@code brightness ≤ this} → ocean. */
  public static final double THRESHOLD_OCEAN_MAX =
    (BAND_OCEAN + BAND_ISLAND) / 2.0;

  /** Midpoint island↔fresh lake. {@code brightness ≤ this} → island. */
  public static final double THRESHOLD_ISLAND_MAX =
    (BAND_ISLAND + BAND_LAKE) / 2.0;

  /** Midpoint fresh lake↔salt lake. {@code brightness ≤ this} → fresh lake. */
  public static final double THRESHOLD_LAKE_MAX =
    (BAND_LAKE + BAND_SALT_LAKE) / 2.0;

  /**
   * Midpoint salt lake↔land. {@code brightness ≤ this} → salt lake;
   * above → land. Classic white (255) stays land.
   */
  public static final double THRESHOLD_SALT_LAKE_MAX =
    (BAND_SALT_LAKE + BAND_LAND) / 2.0;

  /** Hard land sample in the 0–10 continent-noise scale. */
  public static final double LAND_NOISE = 10.0;

  /** Hard ocean sample in the 0–10 continent-noise scale. */
  public static final double OCEAN_NOISE = 0.0;

  public enum ContinentBand {
    OCEAN,
    ISLAND,
    LAKE,
    SALT_LAKE,
    LAND,
  }

  /** Without lakes from the map its lake pixels are land. */
  private final boolean lakesFromMap = TFCRealWorldConfig.LAKES_FROM_MAP.get();

  public PNGContinentNoise(int horizontalScale, int verticalScale) {
    super(
      horizontalScale,
      verticalScale,
      MAP_NAME,
      "Failed to load continent map. Map file is required when generating continents from map."
    );
  }

  private ContinentBand band(double brightness) {
    final ContinentBand band = bandFromBrightness(brightness);
    return (
        !lakesFromMap &&
        (band == ContinentBand.LAKE || band == ContinentBand.SALT_LAKE)
      )
      ? ContinentBand.LAND
      : band;
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
    if (brightness <= THRESHOLD_SALT_LAKE_MAX) {
      return ContinentBand.SALT_LAKE;
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
    return band(sampleGrayAtWorldRounded(gridX, gridZ));
  }

  public ContinentBand bandAtPixel(int x, int z) {
    return band(getBrightness(x, z));
  }

  /**
   * Non-ocean sample (island / lake / land). Used by distance caches and biome
   * land/ocean correction. Classic white land and new mid bands all count.
   */
  public boolean isLandAtGridHard(double gridX, double gridZ) {
    return bandAtGridHard(gridX, gridZ) != ContinentBand.OCEAN;
  }

  /** Whether the map has a lake, fresh or salt, at the grid position. */
  public boolean isLakeAtGridHard(double gridX, double gridZ) {
    final ContinentBand band = bandAtGridHard(gridX, gridZ);
    return band == ContinentBand.LAKE || band == ContinentBand.SALT_LAKE;
  }

  /** Pixel-space non-ocean test (same bands as {@link #isLandAtGridHard}). */
  public boolean isLandPixel(int x, int z) {
    return bandAtPixel(x, z) != ContinentBand.OCEAN;
  }

  /**
   * Whether any island-band pixel lies inside region cell {@code [x, x + 1) ×
   * [z, z + 1)}; catches islands smaller than a cell.
   */
  public boolean anyIslandInCell(int gridX, int gridZ) {
    final double[] min = tileToImage(gridX, gridZ);
    final double[] max = tileToImage(gridX + 1, gridZ + 1);
    final int x0 = (int) Math.floor(Math.min(min[0], max[0]));
    final int x1 = (int) Math.ceil(Math.max(min[0], max[0]));
    final int z0 = (int) Math.floor(Math.min(min[1], max[1]));
    final int z1 = (int) Math.ceil(Math.max(min[1], max[1]));
    for (int z = Math.max(0, z0); z < Math.min(height, z1); z++) {
      for (int x = Math.max(0, x0); x < Math.min(width, x1); x++) {
        if (bandAtPixel(x, z) == ContinentBand.ISLAND) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isOceanAtGridHard(double gridX, double gridZ) {
    return bandAtGridHard(gridX, gridZ) == ContinentBand.OCEAN;
  }
}
