package net.yazloysasha.tfcrealworld.test.drawing;

import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

/** Whether a region grid cell lies inside the primary (center) PNG map tile */
public final class MapTileGridBounds {

  private MapTileGridBounds() {}

  /** Whether the cell lies in the map or within {@code padding} of it. */
  public static boolean isNearPrimaryMapTile(
    int gridX,
    int gridZ,
    int padding
  ) {
    final int radiusX =
      TFCRealWorldConfig.HORIZONTAL_SCALE.get() / Units.GRID_WIDTH_IN_BLOCK;
    final int radiusZ =
      TFCRealWorldConfig.VERTICAL_SCALE.get() / Units.GRID_WIDTH_IN_BLOCK;
    return (
      gridX >= -radiusX - padding &&
      gridX <= radiusX + padding &&
      gridZ >= -radiusZ - padding &&
      gridZ <= radiusZ + padding
    );
  }

  public static boolean isInsidePrimaryMapTile(int gridX, int gridZ) {
    final double tileRadiusGridX =
      TFCRealWorldConfig.HORIZONTAL_SCALE.get() /
      (double) Units.GRID_WIDTH_IN_BLOCK;
    final double tileRadiusGridZ =
      TFCRealWorldConfig.VERTICAL_SCALE.get() /
      (double) Units.GRID_WIDTH_IN_BLOCK;
    return isInsidePrimaryMapTile(
      gridX,
      gridZ,
      tileRadiusGridX,
      tileRadiusGridZ
    );
  }

  public static boolean isInsidePrimaryMapTile(
    int gridX,
    int gridZ,
    double tileRadiusGridX,
    double tileRadiusGridZ
  ) {
    if (tileRadiusGridX <= 0 || tileRadiusGridZ <= 0) {
      return true;
    }
    final int tileX = (int) Math.floor(
      (gridX + tileRadiusGridX) / (2.0 * tileRadiusGridX)
    );
    final int tileZ = (int) Math.floor(
      (gridZ + tileRadiusGridZ) / (2.0 * tileRadiusGridZ)
    );
    return tileX == 0 && tileZ == 0;
  }
}
