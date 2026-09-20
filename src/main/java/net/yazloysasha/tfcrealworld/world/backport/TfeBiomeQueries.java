package net.yazloysasha.tfcrealworld.world.backport;

import static com.newterraearth.tfe.world.NTELayerIds.ICE_SHEET;
import static com.newterraearth.tfe.world.NTELayerIds.ICE_SHEET_TUYAS;
import static com.newterraearth.tfe.world.NTELayerIds.MELTWATER_LAKE;
import static com.newterraearth.tfe.world.NTELayerIds.RIFT_LAKE;
import static com.newterraearth.tfe.world.NTELayerIds.SUBGLACIAL_LAKE;
import static com.newterraearth.tfe.world.NTELayerIds.TOWER_KARST_LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.MOUNTAIN_LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.OCEANIC_MOUNTAIN_LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.OLD_MOUNTAIN_LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.PLATEAU_LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.VOLCANIC_MOUNTAIN_LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.VOLCANIC_OCEANIC_MOUNTAIN_LAKE;

/**
 * TFC 4 {@code TFCLayers.isLake} / {@code isFlatIceSheet} against TFE layer ids.
 * TFC 3 {@code TFCLayers} has no public {@code isLake}.
 */
public final class TfeBiomeQueries {

  private TfeBiomeQueries() {}

  public static boolean isLake(int biome) {
    return (
      biome == LAKE ||
      biome == MELTWATER_LAKE ||
      biome == MOUNTAIN_LAKE ||
      biome == OCEANIC_MOUNTAIN_LAKE ||
      biome == PLATEAU_LAKE ||
      biome == OLD_MOUNTAIN_LAKE ||
      biome == SUBGLACIAL_LAKE ||
      biome == RIFT_LAKE ||
      biome == TOWER_KARST_LAKE ||
      biome == VOLCANIC_OCEANIC_MOUNTAIN_LAKE ||
      biome == VOLCANIC_MOUNTAIN_LAKE
    );
  }

  public static boolean isFlatIceSheet(int biome) {
    return (
      biome == ICE_SHEET || biome == ICE_SHEET_TUYAS || biome == SUBGLACIAL_LAKE
    );
  }
}
