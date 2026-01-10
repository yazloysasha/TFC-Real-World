package net.yazloysasha.tfcrealworld.util.projection;

public interface MapProjectionStrategy {
  int[] geographicToMinecraft(
    double spawnCenterLongitude,
    double spawnCenterLatitude,
    int horizontalTileSize,
    int verticalTileSize,
    double westEdgeLongitude,
    double eastEdgeLongitude,
    double southEdgeLatitude,
    double northEdgeLatitude,
    double tileCenterLongitude,
    double tileCenterLatitude
  );

  double[] minecraftToGeographic(
    int spawnCenterX,
    int spawnCenterZ,
    int horizontalTileSize,
    int verticalTileSize,
    double westEdgeLongitude,
    double eastEdgeLongitude,
    double southEdgeLatitude,
    double northEdgeLatitude,
    double tileCenterLongitude,
    double tileCenterLatitude
  );
}
