package com.newterraearth.tfe.world.terrain;

import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.noise.Noise2D;

public final class NTETerrainUpliftSampler {

  private final long seed;

  public NTETerrainUpliftSampler(long seed, BiomeSourceExtension biomeSource) {
    this.seed = seed;
  }

  private Noise2D activeShieldVolcanoSourceNoise() {
    return (x, z) -> 0;
  }

  private Noise2D dormantShieldVolcanoSourceNoise() {
    return (x, z) -> 0;
  }

  private Noise2D ancientShieldVolcanoSourceNoise() {
    return (x, z) -> 0;
  }

  private Noise2D shieldVolcanoIntensitySourceNoise() {
    return (x, z) -> 0;
  }
}
