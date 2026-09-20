package net.yazloysasha.tfcrealworld.world.volcano;

import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.*;

import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.biome.CoverageRareBiomes;
import su.terrafirmagreg.core.world.new_ow_wg.noise.TFGCellular2D;

public final class TfgCenteredFeatureAligner {

  private TfgCenteredFeatureAligner() {}

  public static void align(Region region, long seed) {
    CenteredFeatureAligner.stamp(
      region,
      cellLookup(
        new TFGCellular2D(seed).spread(
          (float) VolcanoCenteredConstants.SHIELD_CINDER_SPREAD
        )
      ),
      TfgCenteredFeatureAligner::isShieldCinderBiome,
      TfgCenteredFeatureAligner::skipCenter
    );
  }

  private static CenteredFeatureAligner.CellLookup cellLookup(
    TFGCellular2D cells
  ) {
    return (blockX, blockZ) -> {
      final TFGCellular2D.TFGCell cell = cells.cell(blockX, blockZ);
      return new CenteredFeatureAligner.SampledCell(cell.x(), cell.y());
    };
  }

  private static boolean skipCenter(Region.Point center, int sourceBiome) {
    if (isShieldHotspotBiome(center.biome)) {
      return true;
    }
    if (isIceMountainBiome(center.biome)) {
      return true;
    }
    if (isLakeBiome(center.biome) || center.lake()) {
      return true;
    }
    return CoverageRareBiomes.preserve(center.biome);
  }

  private static boolean isShieldHotspotBiome(int biome) {
    return (
      biome == ACTIVE_SHIELD_VOLCANO ||
      biome == DORMANT_SHIELD_VOLCANO ||
      biome == EXTINCT_SHIELD_VOLCANO ||
      biome == ANCIENT_SHIELD_VOLCANO ||
      biome == SUNKEN_SHIELD_VOLCANO ||
      biome == ICE_SHEET_SHIELD_VOLCANO ||
      biome == GLACIATED_SHIELD_VOLCANO ||
      biome == SHIELD_VOLCANO_SHORE ||
      biome == OLD_SHIELD_VOLCANO_SHORE
    );
  }

  private static boolean isShieldCinderBiome(int biome) {
    return (
      biome == ACTIVE_SHIELD_VOLCANO ||
      biome == VOLCANIC_MOUNTAINS ||
      biome == VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == VOLCANIC_MOUNTAIN_LAKE ||
      biome == VOLCANIC_OCEANIC_MOUNTAIN_LAKE
    );
  }

  private static boolean isIceMountainBiome(int biome) {
    return (
      biome == ICE_SHEET_MOUNTAINS ||
      biome == ICE_SHEET_OCEANIC_MOUNTAINS ||
      biome == GLACIATED_MOUNTAINS ||
      biome == GLACIATED_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_MOUNTAINS ||
      biome == GLACIALLY_CARVED_OCEANIC_MOUNTAINS
    );
  }

  private static boolean isLakeBiome(int biome) {
    return (
      biome == LAKE ||
      biome == MOUNTAIN_LAKE ||
      biome == OLD_MOUNTAIN_LAKE ||
      biome == OCEANIC_MOUNTAIN_LAKE ||
      biome == VOLCANIC_MOUNTAIN_LAKE ||
      biome == VOLCANIC_OCEANIC_MOUNTAIN_LAKE ||
      biome == PLATEAU_LAKE ||
      biome == SUBGLACIAL_LAKE ||
      biome == MELTWATER_LAKE ||
      biome == TOWER_KARST_LAKE
    );
  }
}
