package net.yazloysasha.tfcrealworld.test.drawing;

import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

/**
 * Whether a region grid cell lies inside the primary (center) PNG map tile
 */
public final class MapTileGridBounds {

  private MapTileGridBounds() {}

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
    if (tileX != 0 || tileZ != 0) {
      return false;
    }
    final double tileCenterX = tileX * 2.0 * tileRadiusGridX;
    final double tileCenterZ = tileZ * 2.0 * tileRadiusGridZ;
    double localX = gridX - tileCenterX;
    double localZ = gridZ - tileCenterZ;
    if (Math.floorMod(tileX, 2) != 0) {
      localX = -localX;
    }
    if (Math.floorMod(tileZ, 2) != 0) {
      localZ = -localZ;
    }
    return (
      Math.abs(localX) <= tileRadiusGridX && Math.abs(localZ) <= tileRadiusGridZ
    );
  }
}
