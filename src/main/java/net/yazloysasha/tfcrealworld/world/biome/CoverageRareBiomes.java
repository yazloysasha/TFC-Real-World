package net.yazloysasha.tfcrealworld.world.biome;

import static net.dries007.tfc.world.layer.TFCLayers.OCEANIC_MOUNTAIN_LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.VOLCANIC_OCEANIC_MOUNTAIN_LAKE;

/**
 * Biomes that are sparse on full-map coverage runs and should not be overwritten by
 * map rift cores or volcanic center alignment. Generic mountains are not included.
 *
 * <p>Same checks as 1.21.1. TFC 3 has no {@code TFCLayers.TUYAS} etc., so extra
 * 1.21 ids are read from TerraFirmaEarth {@code NTELayerIds} or Core-Modern
 * {@code TFGLayers} at call time. When both mods are loaded, match either
 * numeric id so TFG biomes are not compared against TFE's assignment table.
 */
public final class CoverageRareBiomes {

  private static final String TFE_LAYERS =
    "com.newterraearth.tfe.world.NTELayerIds";
  private static final String TFG_LAYERS =
    "su.terrafirmagreg.core.world.new_ow_wg.TFGLayers";

  private CoverageRareBiomes() {}

  public static boolean preserve(int biome) {
    if (isLayer(biome, "TUYAS")) {
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
      isLayer(biome, "OCEANIC_MOUNTAIN_LAKE") ||
      isLayer(biome, "VOLCANIC_OCEANIC_MOUNTAIN_LAKE") ||
      isLayer(biome, "GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS") ||
      isLayer(biome, "GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS") ||
      isLayer(biome, "MELTWATER_LAKE")
    );
  }

  private static boolean preserveBurrenAndGlacialKarst(int biome) {
    return (
      isLayer(biome, "DRUMLINS") ||
      isLayer(biome, "BURREN_PLAINS") ||
      isLayer(biome, "BURREN_BADLANDS") ||
      isLayer(biome, "BURREN_BADLANDS_TALL") ||
      isLayer(biome, "BURREN_PLATEAU") ||
      isLayer(biome, "BURREN_ROCHE_MOUTONEE") ||
      isLayer(biome, "TOWER_KARST_BAY")
    );
  }

  private static boolean isLayer(int biome, String name) {
    Integer tfe = staticInt(TFE_LAYERS, name);
    if (tfe != null && tfe == biome) {
      return true;
    }
    Integer tfg = staticInt(TFG_LAYERS, name);
    return tfg != null && tfg == biome;
  }

  private static Integer staticInt(String className, String field) {
    try {
      return Class.forName(className).getField(field).getInt(null);
    } catch (ReflectiveOperationException | LinkageError ignored) {
      return null;
    }
  }
}
