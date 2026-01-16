package net.yazloysasha.tfcrealworld.util.projection;

public interface MapProjectionStrategy {
  double getLatitudeByZ(
    double z,
    int verticalTileSize,
    double southEdgeLatitude,
    double northEdgeLatitude,
    double tileCenterLongitude
  );

  double[] geographicToClassic(
    double longitude,
    double latitude,
    int horizontalTileSize,
    int verticalTileSize,
    double westEdgeLongitude,
    double eastEdgeLongitude,
    double southEdgeLatitude,
    double northEdgeLatitude,
    double tileCenterLongitude,
    double tileCenterLatitude
  );

  double[] classicToGeographic(
    double x,
    double z,
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
