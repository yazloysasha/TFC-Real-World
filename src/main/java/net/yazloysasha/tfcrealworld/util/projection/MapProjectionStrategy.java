package net.yazloysasha.tfcrealworld.util.projection;

public interface MapProjectionStrategy {
  double getLatitudeByZ(
    double z,
    int verticalScale,
    double southEdgeLatitude,
    double northEdgeLatitude,
    double tileCenterLongitude
  );

  double[] geographicToClassic(
    double longitude,
    double latitude,
    int horizontalScale,
    int verticalScale,
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
    int horizontalScale,
    int verticalScale,
    double westEdgeLongitude,
    double eastEdgeLongitude,
    double southEdgeLatitude,
    double northEdgeLatitude,
    double tileCenterLongitude,
    double tileCenterLatitude
  );
}
