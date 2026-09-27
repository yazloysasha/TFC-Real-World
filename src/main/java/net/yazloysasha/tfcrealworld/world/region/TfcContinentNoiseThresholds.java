package net.yazloysasha.tfcrealworld.world.region;

/**
 * Land/ocean cutoffs from vanilla TFC {@code AddContinentsAndSetOceanDepths}.
 * TFC does not expose these as named constants or config; shared copy here.
 */
public final class TfcContinentNoiseThresholds {

  /**
   * Vanilla: {@code if (continent > 4.4) point.setLand()}.
   */
  public static final double LAND = 4.4;

  /**
   * Vanilla: {@code else if (continent > 3.3) oceanDepth = 2} (shelf).
   */
  public static final double CONTINENTAL_SHELF = 3.3;

  private TfcContinentNoiseThresholds() {}
}
