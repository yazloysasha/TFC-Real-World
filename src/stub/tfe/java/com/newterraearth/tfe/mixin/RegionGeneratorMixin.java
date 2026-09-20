package com.newterraearth.tfe.mixin;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.settings.Settings;

public abstract class RegionGeneratorMixin {

  private Noise2D tfe$hotSpotAgeNoise;
  private Noise2D tfe$hotSpotIntensityNoise;
  private Noise2D tfe$rainfallVarianceNoise;

  public float nte$continentFactor(int gridX, int gridZ) {
    return 1f;
  }

  public double nte$getDivergence(int gridX, int gridZ) {
    return 0d;
  }

  private void tfe$reinitExtraRegionNoise(long rootLevelSeed) {}

  public void tfe$initExtraRegionNoise(
    Settings settings,
    net.minecraft.util.RandomSource random
  ) {}
}
