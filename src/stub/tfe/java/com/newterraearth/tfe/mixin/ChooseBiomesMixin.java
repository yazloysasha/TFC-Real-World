package com.newterraearth.tfe.mixin;

import net.dries007.tfc.world.region.RegionGenerator;

public abstract class ChooseBiomesMixin {

  public void apply(RegionGenerator.Context context) {}

  private int getHotSpotBiome(int age) {
    return 0;
  }

  private int getBurrenBiome(int biome) {
    return biome;
  }

  private int getTowerKarstBiome(int biome) {
    return biome;
  }

  private int randomSeededFrom(long rngSeed, int areaSeed, int[] choices) {
    return 0;
  }
}
