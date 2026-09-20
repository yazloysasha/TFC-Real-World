package net.yazloysasha.tfcrealworld.world.volcano;

import static com.newterraearth.tfe.world.NTELayerIds.*;
import static net.dries007.tfc.world.layer.TFCLayers.VOLCANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.VOLCANIC_MOUNTAIN_LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.VOLCANIC_OCEANIC_MOUNTAIN_LAKE;

import com.newterraearth.tfe.world.noise.NTECellular2D;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.backport.TfeBiomeQueries;
import net.yazloysasha.tfcrealworld.world.biome.CoverageRareBiomes;

public final class TfeCenteredFeatureAligner {

  private TfeCenteredFeatureAligner() {}

  public static void align(Region region, long seed) {
    CenteredFeatureAligner.stamp(
      region,
      cellLookup(
        new NTECellular2D(seed, 2).spread(
          (float) VolcanoCenteredConstants.STRATOVOLCANO_SPREAD
        )
      ),
      TfeCenteredFeatureAligner::isStratovolcanoBiome,
      TfeCenteredFeatureAligner::skipCenter
    );
    CenteredFeatureAligner.stamp(
      region,
      cellLookup(
        new NTECellular2D(seed).spread(
          (float) VolcanoCenteredConstants.SHIELD_CINDER_SPREAD
        )
      ),
      TfeCenteredFeatureAligner::isShieldCinderBiome,
      TfeCenteredFeatureAligner::skipCenter
    );
  }

  private static CenteredFeatureAligner.CellLookup cellLookup(
    NTECellular2D cells
  ) {
    return (blockX, blockZ) -> {
      final NTECellular2D.Cell cell = cells.cell(blockX, blockZ);
      return new CenteredFeatureAligner.SampledCell(cell.x(), cell.y());
    };
  }

  private static boolean skipCenter(Region.Point center, int sourceBiome) {
    if (TfeVolcanoMapPipeline.isShieldHotspotBiome(center.biome)) {
      return true;
    }
    if (TfeBiomeQueries.isLake(center.biome) || center.lake()) {
      return true;
    }
    return CoverageRareBiomes.preserve(center.biome);
  }

  private static boolean isStratovolcanoBiome(int biome) {
    return (
      biome == OCEANIC_VOLCANIC_ARC ||
      biome == VOLCANIC_MOUNTAINS ||
      biome == VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == VOLCANIC_ISLAND ||
      biome == VOLCANIC_MOUNTAIN_LAKE ||
      biome == VOLCANIC_OCEANIC_MOUNTAIN_LAKE ||
      biome == ICE_SHEET_VOLCANIC_MOUNTAINS ||
      biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
    );
  }

  private static boolean isShieldCinderBiome(int biome) {
    return biome == ACTIVE_SHIELD_VOLCANO || biome == VOLCANIC_MOUNTAIN_ISLANDS;
  }
}
