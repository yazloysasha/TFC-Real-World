package net.yazloysasha.tfcrealworld.util.projection;

/**
 * Equal Earth projection implementation for converting geographic coordinates to Minecraft coordinates.
 * Uses the exact mathematical formulas of the Equal Earth projection.
 */
public class EqualEarthProjectionStrategy implements MapProjectionStrategy {

  private static final double A1 = 1.340264;
  private static final double A2 = -0.081106;
  private static final double A3 = 0.000893;
  private static final double A4 = 0.003796;
  private static final double SQRT_3 = Math.sqrt(3.0);
  private static final double R = 1.0;

  @Override
  public int[] geographicToMinecraft(
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
  ) {
    spawnCenterLongtitude = normalizeLongitude(spawnCenterLongtitude);
    spawnCenterLatitude = Math.clamp(spawnCenterLatitude, -90.0, 90.0);

    double lambda0 = Math.toRadians(tileCenterLongitude);
    double lambda = Math.toRadians(spawnCenterLongtitude);
    double phi = Math.toRadians(spawnCenterLatitude);

    double deltaLambda = lambda - lambda0;

    while (deltaLambda > Math.PI) deltaLambda -= 2.0 * Math.PI;
    while (deltaLambda < -Math.PI) deltaLambda += 2.0 * Math.PI;

    double[] projCoords = forwardProjection(phi, deltaLambda);
    double projX = projCoords[0];
    double projY = projCoords[1];

    double westLonRad = Math.toRadians(westEdgeLongitude);
    double eastLonRad = Math.toRadians(eastEdgeLongitude);
    double southLatRad = Math.toRadians(southEdgeLatitude);
    double northLatRad = Math.toRadians(northEdgeLatitude);
    double centralLatRad = Math.toRadians(tileCenterLatitude);

    double deltaWestLon = westLonRad - lambda0;
    double deltaEastLon = eastLonRad - lambda0;

    while (deltaWestLon > Math.PI) deltaWestLon -= 2.0 * Math.PI;
    while (deltaWestLon < -Math.PI) deltaWestLon += 2.0 * Math.PI;
    while (deltaEastLon > Math.PI) deltaEastLon -= 2.0 * Math.PI;
    while (deltaEastLon < -Math.PI) deltaEastLon += 2.0 * Math.PI;

    double[] westProj = forwardProjection(centralLatRad, deltaWestLon);
    double[] eastProj = forwardProjection(centralLatRad, deltaEastLon);
    double westProjX = westProj[0];
    double eastProjX = eastProj[0];

    double[] southProj = forwardProjection(southLatRad, 0.0);
    double[] northProj = forwardProjection(northLatRad, 0.0);
    double southProjY = southProj[1];
    double northProjY = northProj[1];

    double projWidth = Math.abs(eastProjX - westProjX);
    double projHeight = Math.abs(northProjY - southProjY);

    double normalizedX = (projX - westProjX) / projWidth;
    normalizedX = Math.clamp(normalizedX, 0.0, 1.0);

    double normalizedY = (projY - southProjY) / projHeight;
    normalizedY = Math.clamp(normalizedY, 0.0, 1.0);

    normalizedY = 1.0 - normalizedY;

    int x = (int) Math.round(
      normalizedX * horizontalTileSize - horizontalTileSize / 2.0
    );
    int z = (int) Math.round(
      normalizedY * verticalTileSize - verticalTileSize / 2.0
    );

    return new int[] { x, z };
  }

  /**
   * Forward projection of Equal Earth.
   * Converts geographic coordinates (latitude, longitude difference) to projection coordinates (x, y).
   *
   * @param phi latitude in radians
   * @param deltaLambda longitude difference from central meridian in radians
   * @return array [x, y] of projection coordinates
   */
  private static double[] forwardProjection(double phi, double deltaLambda) {
    double sinPhi = Math.sin(phi);
    double theta = Math.asin((SQRT_3 / 2.0) * sinPhi);

    double theta2 = theta * theta;
    double theta6 = theta2 * theta2 * theta2;
    double theta8 = theta6 * theta2;
    double denominator =
      9.0 * A4 * theta8 + 7.0 * A3 * theta6 + 3.0 * A2 * theta2 + A1;

    double cosTheta = Math.cos(theta);
    double x =
      (2.0 * SQRT_3 * R * deltaLambda * cosTheta) / (3.0 * denominator);
    double y = R * theta * (A1 + A2 * theta2 + theta6 * (A3 + A4 * theta2));

    return new double[] { x, y };
  }

  private static double normalizeLongitude(double longitude) {
    while (longitude > 180.0) longitude -= 360.0;
    while (longitude < -180.0) longitude += 360.0;
    return longitude;
  }
}
