package net.yazloysasha.tfcrealworld.world.biome;

import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.*;

/**
 * Biomes that are sparse on full-map coverage and should not be overwritten by
 * volcanic center alignment. Same set as 1.21.1 {@code CoverageRareBiomes},
 * minus glacial volcanic oceanic mountains TFG does not have. Generic mountains
 * are not included.
 */
public final class CoverageRareBiomes {

  private CoverageRareBiomes() {}

  public static boolean preserve(int biome) {
    if (
      biome == TUYAS ||
      biome == ICE_SHEET_TUYAS ||
      biome == ICE_SHEET_TUYAS_EDGE
    ) {
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
