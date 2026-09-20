package com.newterraearth.tfe.world.region;

import net.dries007.tfc.world.noise.Noise2D;

public final class NTERegionNoise {

  private NTERegionNoise() {}

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

  public static Noise2D hotSpotIntensity(long seed) {
    return (x, z) -> 0;
  }

  public static Noise2D hotSpotAge(long seed) {
    return (x, z) -> 0;
  }
}
