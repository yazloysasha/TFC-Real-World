package net.yazloysasha.tfcrealworld.world.region;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.dries007.tfc.world.biome.BiomeBlendType;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.region.Region;

public final class BiomePools {

  public final boolean[] isVolcanicLayer;

  final int[] nonVolcanicOcean;
  final int[] nonVolcanicLand;

  private BiomePools(
    boolean[] isVolcanicLayer,
    int[] nonVolcanicOcean,
    int[] nonVolcanicLand
  ) {
    this.isVolcanicLayer = isVolcanicLayer;
    this.nonVolcanicOcean = nonVolcanicOcean;
    this.nonVolcanicLand = nonVolcanicLand;
  }

  public static BiomePools build() {
    final boolean[] isVolcanicLayer = new boolean[64];
    final IntArrayList nonOcean = new IntArrayList();
    final IntArrayList nonLand = new IntArrayList();

    for (int id = 0; id < 64; id++) {
      final BiomeExtension ext;
      try {
        ext = TFCLayers.getFromLayerId(id);
      } catch (Throwable t) {
        continue;
      }

      if (ext.biomeBlendType() == BiomeBlendType.LAKE) continue;
      if (ext.createNoiseSampler(0L) == null) continue;

      final boolean volcanic = ext.isVolcanic();
      isVolcanicLayer[id] = volcanic;
      if (volcanic) {
        continue;
      }

      final boolean oceanLike =
        ext.isSalty() || ext.biomeBlendType() == BiomeBlendType.OCEAN;
      if (oceanLike) {
        nonOcean.add(id);
      } else {
        nonLand.add(id);
      }
    }

    return new BiomePools(
      isVolcanicLayer,
      nonOcean.toIntArray(),
      nonLand.toIntArray()
    );
  }

  public int pickNonVolcanic(
    Region.Point point,
    int x,
    int z,
    int originalBiome
  ) {
    if (!point.land()) {
      return pickOrOriginal(nonVolcanicOcean, x, z, 12, originalBiome);
    }

    final int altitude = point.discreteBiomeAltitude();
    if (altitude <= 0) {
      return pickOrOriginal(nonVolcanicLand, x, z, 22, originalBiome);
    } else if (altitude == 1) {
      return pickOrOriginal(nonVolcanicLand, x, z, 32, originalBiome);
    } else {
      return pickOrOriginal(nonVolcanicLand, x, z, 42, originalBiome);
    }
  }

  private static int pickOrOriginal(
    int[] pool,
    int x,
    int z,
    int salt,
    int originalBiome
  ) {
    if (pool.length == 0) return originalBiome;
    int h = (x * 73428767) ^ (z * 912931) ^ (salt * 104395301);
    h ^= (h >>> 16);
    h *= 0x7feb352d;
    h ^= (h >>> 15);
    h *= 0x846ca68b;
    h ^= (h >>> 16);
    return pool[Math.floorMod(h, pool.length)];
  }
}
