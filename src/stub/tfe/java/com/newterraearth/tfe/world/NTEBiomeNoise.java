package com.newterraearth.tfe.world;

import net.dries007.tfc.world.noise.Noise2D;

public class NTEBiomeNoise {

  public static Noise2D dolineCanyons(long seed) {
    return (x, z) -> 0;
  }

  public static Noise2D activeShieldVolcano(long seed) {
    return activeShieldVolcano(seed, (x, z) -> 0);
  }

  public static Noise2D dormantShieldVolcano(long seed) {
    return dormantShieldVolcano(seed, (x, z) -> 0);
  }

  public static Noise2D extinctShieldVolcano(long seed) {
    return extinctShieldVolcano(seed, (x, z) -> 0);
  }

  public static Noise2D ancientShieldVolcano(long seed) {
    return ancientShieldVolcano(seed, 90.0, 130.0, (x, z) -> 0);
  }

  public static Noise2D sunkenShieldVolcano(long seed) {
    return sunkenShieldVolcano(seed, (x, z) -> 0);
  }

  public static Noise2D glaciatedShieldVolcano(long seed) {
    return glaciatedShieldVolcano(seed, (x, z) -> 0);
  }

  public static Noise2D shieldVolcanoIceSheetSurface(long seed) {
    return shieldVolcanoIceSheetSurface(seed, (x, z) -> 0);
  }

  public static Noise2D shieldVolcanoGlacierSurface(long seed) {
    return shieldVolcanoGlacierSurface(seed, (x, z) -> 0);
  }

  private static Noise2D activeShieldVolcano(long seed, Noise2D intensity) {
    return intensity;
  }

  private static Noise2D dormantShieldVolcano(long seed, Noise2D intensity) {
    return intensity;
  }

  private static Noise2D extinctShieldVolcano(long seed, Noise2D intensity) {
    return intensity;
  }

  private static Noise2D ancientShieldVolcano(
    long seed,
    double min,
    double max,
    Noise2D intensity
  ) {
    return intensity;
  }

  private static Noise2D sunkenShieldVolcano(long seed, Noise2D intensity) {
    return intensity;
  }

  private static Noise2D glaciatedShieldVolcano(long seed, Noise2D intensity) {
    return intensity;
  }

  private static Noise2D shieldVolcanoIceSheetSurface(
    long seed,
    Noise2D intensity
  ) {
    return intensity;
  }

  private static Noise2D shieldVolcanoGlacierSurface(
    long seed,
    Noise2D intensity
  ) {
    return intensity;
  }

  private static Noise2D glacialSurfaceTexture(long seed) {
    return (x, z) -> 0;
  }
}
