package su.terrafirmagreg.core.world.new_ow_wg.noise;

import net.dries007.tfc.world.noise.Noise2D;

/**
 * Compile-only stub matching TFG hotspot intensity factories.
 */
public class TFGBiomeNoise {

  public static Noise2D activeHotSpots(long seed) {
    return (x, z) -> 0;
  }

  public static Noise2D dormantHotSpots(long seed) {
    return (x, z) -> 0;
  }

  public static Noise2D extinctHotSpots(long seed) {
    return (x, z) -> 0;
  }

  public static Noise2D ancientHotSpots(long seed) {
    return (x, z) -> 0;
  }

  public static Noise2D bowlDolines(
    long seed,
    Noise2D baseTerrainNoise,
    double scale
  ) {
    return baseTerrainNoise;
  }
}
