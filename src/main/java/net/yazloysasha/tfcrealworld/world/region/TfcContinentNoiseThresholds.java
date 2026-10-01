package net.yazloysasha.tfcrealworld.world.region;

/**
 * Cutoffs from vanilla TFC {@code AddContinentsAndSetOceanDepths} applied to the
 * continuous continent-noise value. Used only when continents come from the
 * map but tectonics are procedural; with {@code tectonics.png} ocean depth is
 * a property of the tectonic class instead.
 */
public final class TfcContinentNoiseThresholds {

  /**
   * Vanilla: {@code if (continent > 4.4) point.setLand()}. Map mode uses
   * continent.png bands for land/island/lake instead.
   */
  public static final double LAND = 4.4;

  /**
   * Vanilla: {@code else if (continent > 3.3) oceanDepth = 2} (shelf).
   */
  public static final double CONTINENTAL_SHELF = 3.3;

  /**
   * Vanilla: {@code else if (continent > 3 && divergence < 0) oceanDepth = 5}
   * (trench). Effective window is {@code (3, 3.3]} because shelf wins above
   * 3.3.
   */
  public static final double TRENCH_CONTINENT = 3.0;

  private TfcContinentNoiseThresholds() {}
}
