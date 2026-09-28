package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.config.BiomeStubs;

/**
 * Marks vanilla ICE_SHEET_EDGE cells as lakes before ChooseBiomes runs
 */
public final class MeltwaterLakeAnnotator {

  private static final float ICE_SHEET_TEMP_BASE = -17f;
  private static final float ICE_SHEET_TEMP_RAIN_SCALE = 0.006f;
  private static final float ICE_SHEET_EDGE_BAND = 1f;

  private MeltwaterLakeAnnotator() {}

  public static void apply(Region region) {
    if (!BiomeStubs.PATCH_MELTWATER_LAKE) {
      return;
    }
    for (final Region.Point point : region.points()) {
      if (point == null || !matchesIceSheetEdgeMeltwater(point)) {
        continue;
      }
      point.setLake();
    }
  }

  /**
   * Matches the non-mountain land branch of ChooseBiomes' ICE_SHEET_EDGE band.
   * Distance to ocean only changes flat ice below this band; it does not gate
   * ICE_SHEET_EDGE. Exclude earlier biome branches and hotspot replacements so
   * the final lakeFor call receives ICE_SHEET_EDGE.
   */
  public static boolean matchesIceSheetEdgeMeltwater(Region.Point point) {
    if (
      !point.land() ||
      point.island() ||
      point.mountain() ||
      point.hotSpotAge > 0
    ) {
      return false;
    }
    if (point.distanceToEdge < 3 && point.divergence > 0f) {
      return false;
    }
    final float maxIceSheetTemp =
      ICE_SHEET_TEMP_BASE + ICE_SHEET_TEMP_RAIN_SCALE * point.rainfall;
    return (
      point.temperature >= maxIceSheetTemp &&
      point.temperature < maxIceSheetTemp + ICE_SHEET_EDGE_BAND
    );
  }
}
