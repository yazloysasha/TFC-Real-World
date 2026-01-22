package net.yazloysasha.tfcrealworld.world.region;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.region.Region;

public final class BiomePools {

  public final boolean[] isVolcanicLayer;
  final int[] volcanicOcean;
  final int[] volcanicLand;

  final int[] nonVolcanicOcean;
  final int[] nonVolcanicLand;

  private BiomePools(
    boolean[] isVolcanicLayer,
    int[] volcanicOcean,
    int[] volcanicLand,
    int[] nonVolcanicOcean,
    int[] nonVolcanicLand
  ) {
    this.isVolcanicLayer = isVolcanicLayer;
    this.volcanicOcean = volcanicOcean;
    this.volcanicLand = volcanicLand;
    this.nonVolcanicOcean = nonVolcanicOcean;
    this.nonVolcanicLand = nonVolcanicLand;
  }

  public static BiomePools build() {
    final boolean[] isVolcanicLayer = new boolean[64];
    final IntArrayList volcOcean = new IntArrayList();
    final IntArrayList volcLand = new IntArrayList();

    final IntArrayList nonOcean = new IntArrayList();
    final IntArrayList nonLand = new IntArrayList();

    for (int id = 0; id < 64; id++) {
      final BiomeExtension ext;
      try {
        ext = TFCLayers.getFromLayerId(id);
      } catch (Throwable t) {
        continue;
      }

      if (ext.getGroup() == BiomeExtension.Group.LAKE) continue;
      if (ext.getGroup() == BiomeExtension.Group.RIVER) continue;
      if (ext.createNoiseSampler(0L) == null) continue;

      final boolean volcanic = ext.isVolcanic();
      isVolcanicLayer[id] = volcanic;
      final boolean oceanLike =
        ext.isSalty() || ext.getGroup() == BiomeExtension.Group.OCEAN;

      if (volcanic) {
        if (oceanLike) {
          volcOcean.add(id);
        } else {
          volcLand.add(id);
        }
      } else {
        if (oceanLike) {
          nonOcean.add(id);
        } else {
          nonLand.add(id);
        }
      }
    }

    return new BiomePools(
      isVolcanicLayer,
      volcOcean.toIntArray(),
      volcLand.toIntArray(),
      nonOcean.toIntArray(),
      nonLand.toIntArray()
    );
  }

  public int pickVolcanic(Region.Point point, int x, int z, int originalBiome) {
    final int altitude = point.discreteBiomeAltitude();
    if (!point.land()) {
      return pickOrOriginal(volcanicOcean, x, z, 11, originalBiome);
    }

    return pickOrOriginal(volcanicLand, x, z, 20 + altitude, originalBiome);
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
