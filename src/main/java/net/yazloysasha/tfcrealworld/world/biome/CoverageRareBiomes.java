package net.yazloysasha.tfcrealworld.world.biome;

import static net.dries007.tfc.world.layer.TFCLayers.*;

/**
 * Biomes that are sparse on full-map coverage runs and should not be overwritten by
 * map rift cores or volcanic center alignment. Generic mountains are not included.
 */
public final class CoverageRareBiomes {

  private CoverageRareBiomes() {}

  public static boolean preserve(int biome) {
    if (biome == TUYAS) {
      return true;
    }
    if (preserveVolcanicAndMountainLakes(biome)) {
      return true;
    }
    return preserveBurrenAndGlacialKarst(biome);
  }

  private static boolean preserveVolcanicAndMountainLakes(int biome) {
    return (
      biome == OCEANIC_MOUNTAIN_LAKE ||
      biome == VOLCANIC_OCEANIC_MOUNTAIN_LAKE ||
      biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == MELTWATER_LAKE
    );
  }

  private static boolean preserveBurrenAndGlacialKarst(int biome) {
    return (
      biome == DRUMLINS ||
      biome == BURREN_PLAINS ||
      biome == BURREN_BADLANDS ||
      biome == BURREN_BADLANDS_TALL ||
      biome == BURREN_PLATEAU ||
      biome == BURREN_ROCHE_MOUTONEE ||
      biome == TOWER_KARST_BAY
    );
  }
}
