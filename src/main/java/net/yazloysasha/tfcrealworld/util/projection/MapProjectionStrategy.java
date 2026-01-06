package net.yazloysasha.tfcrealworld.util.projection;

public interface MapProjectionStrategy {
  int geographicToMinecraftX(
    double spawnCenterLongtitude,
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

  int geographicToMinecraftZ(
    double spawnCenterLongtitude,
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
}
