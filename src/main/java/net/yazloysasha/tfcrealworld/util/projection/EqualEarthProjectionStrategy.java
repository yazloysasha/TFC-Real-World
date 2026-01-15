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
  public double getLatitudeByZ(
    double z,
    int verticalTileSize,
    double southEdgeLatitude,
    double northEdgeLatitude,
    double tileCenterLongitude
  ) {
    double normalizedY = 1.0 - (z + verticalTileSize / 2.0) / verticalTileSize;

    double southLatRad = Math.toRadians(southEdgeLatitude);
    double northLatRad = Math.toRadians(northEdgeLatitude);

    double[] southProj = forwardProjection(southLatRad, 0.0);
    double[] northProj = forwardProjection(northLatRad, 0.0);
    double southProjY = southProj[1];
    double northProjY = northProj[1];

    double projHeight = Math.abs(northProjY - southProjY);
    double projY = southProjY + normalizedY * projHeight;

    double lambda0 = Math.toRadians(tileCenterLongitude);
    double projX = 0.0;

    double[] geoCoords = inverseProjection(projX, projY, lambda0);
    double latitude = Math.toDegrees(geoCoords[1]);

    return Math.clamp(latitude, -90.0, 90.0);
  }

  @Override
  public double[] geographicToMinecraft(
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
  ) {
    double[] normalizedEdges = normalizeLongitudeEdges(
      westEdgeLongitude,
      eastEdgeLongitude
    );
    double normalizedWest = normalizedEdges[0];
    double normalizedEast = normalizedEdges[1];

    longitude = normalizeLongitude(longitude);
    latitude = normalizeLatitude(latitude);

    if (westEdgeLongitude > eastEdgeLongitude) {
      if (longitude >= westEdgeLongitude) {
        longitude = longitude - 360.0;
      }
    }

    double normalizedTileCenterLongitude =
      (normalizedWest + normalizedEast) / 2.0;
    double lambda0 = Math.toRadians(normalizedTileCenterLongitude);
    double lambda = Math.toRadians(longitude);
    double phi = Math.toRadians(latitude);

    double deltaLambda = lambda - lambda0;

    while (deltaLambda > Math.PI) deltaLambda -= 2.0 * Math.PI;
    while (deltaLambda < -Math.PI) deltaLambda += 2.0 * Math.PI;

    double[] projCoords = forwardProjection(phi, deltaLambda);
    double projX = projCoords[0];
    double projY = projCoords[1];

    double westLonRad = Math.toRadians(normalizedWest);
    double eastLonRad = Math.toRadians(normalizedEast);
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
    double normalizedY = 1.0 - (projY - southProjY) / projHeight;

    double x = (normalizedX * horizontalTileSize - horizontalTileSize / 2.0);
    double z = (normalizedY * verticalTileSize - verticalTileSize / 2.0);

    return new double[] { x, z };
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

  @Override
  public double[] minecraftToGeographic(
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
  ) {
    double[] normalizedEdges = normalizeLongitudeEdges(
      westEdgeLongitude,
      eastEdgeLongitude
    );
    double normalizedWest = normalizedEdges[0];
    double normalizedEast = normalizedEdges[1];

    double normalizedTileCenterLongitude =
      (normalizedWest + normalizedEast) / 2.0;
    double lambda0 = Math.toRadians(normalizedTileCenterLongitude);
    double westLonRad = Math.toRadians(normalizedWest);
    double eastLonRad = Math.toRadians(normalizedEast);
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

    double normalizedX = (x + horizontalTileSize / 2.0) / horizontalTileSize;
    double normalizedY = 1.0 - (z + verticalTileSize / 2.0) / verticalTileSize;

    double projX = westProjX + normalizedX * projWidth;
    double projY = southProjY + normalizedY * projHeight;

    double[] geoCoords = inverseProjection(projX, projY, lambda0);

    double longitude = Math.toDegrees(geoCoords[0]);
    double latitude = Math.toDegrees(geoCoords[1]);

    longitude = normalizeLongitude(longitude);
    latitude = Math.clamp(latitude, -90.0, 90.0);

    if (westEdgeLongitude > eastEdgeLongitude) {
      if (longitude < normalizedWest) {
        double positiveLongitude = longitude + 360.0;
        if (
          positiveLongitude >= westEdgeLongitude && positiveLongitude <= 180.0
        ) {
          longitude = positiveLongitude;
        }
      }
    }

    return new double[] { longitude, latitude };
  }

  /**
   * Inverse projection of Equal Earth.
   * Converts projection coordinates (x, y) to geographic coordinates (longitude, latitude).
   *
   * @param projX projection X coordinate
   * @param projY projection Y coordinate
   * @param lambda0 central meridian longitude in radians
   * @return array [longitude, latitude] in radians
   */
  private static double[] inverseProjection(
    double projX,
    double projY,
    double lambda0
  ) {
    double theta = solveThetaForY(projY);

    double cosTheta = Math.cos(theta);
    double theta2 = theta * theta;
    double theta6 = theta2 * theta2 * theta2;
    double theta8 = theta6 * theta2;
    double denominator =
      9.0 * A4 * theta8 + 7.0 * A3 * theta6 + 3.0 * A2 * theta2 + A1;

    double deltaLambda =
      (3.0 * denominator * projX) / (2.0 * SQRT_3 * R * cosTheta);

    double phi = Math.asin(
      Math.clamp((2.0 / SQRT_3) * Math.sin(theta), -1.0, 1.0)
    );

    double longitude = lambda0 + deltaLambda;
    double latitude = phi;

    while (longitude > Math.PI) longitude -= 2.0 * Math.PI;
    while (longitude < -Math.PI) longitude += 2.0 * Math.PI;

    return new double[] { longitude, latitude };
  }

  /**
   * Solves for theta given y in the Equal Earth projection.
   * Uses Newton's method to solve: y = R * theta * (A1 + A2 * theta^2 + theta^6 * (A3 + A4 * theta^2))
   *
   * @param y projection Y coordinate
   * @return theta in radians
   */
  private static double solveThetaForY(double y) {
    double targetY = y / R;
    double theta = Math.asin(
      Math.min(1.0, Math.max(-1.0, ((2.0 / SQRT_3) * targetY) / (A1 + 1.0)))
    );

    for (int i = 0; i < 20; i++) {
      double theta2 = theta * theta;
      double theta6 = theta2 * theta2 * theta2;
      double theta8 = theta6 * theta2;

      double f =
        theta * (A1 + A2 * theta2 + theta6 * (A3 + A4 * theta2)) - targetY;
      double fPrime =
        A1 + 3.0 * A2 * theta2 + 7.0 * A3 * theta6 + 9.0 * A4 * theta8;

      if (Math.abs(fPrime) < 1e-10) {
        break;
      }

      double delta = f / fPrime;
      theta = theta - delta;

      if (Math.abs(delta) < 1e-10) {
        break;
      }

      theta = Math.clamp(theta, -Math.PI / 2.0, Math.PI / 2.0);
    }

    return theta;
  }

  private static double[] normalizeLongitudeEdges(
    double westEdgeLongitude,
    double eastEdgeLongitude
  ) {
    if (westEdgeLongitude > eastEdgeLongitude) {
      westEdgeLongitude -= 360.0;
    }

    return new double[] { westEdgeLongitude, eastEdgeLongitude };
  }

  private static double normalizeLongitude(double longitude) {
    while (longitude > 180.0) longitude -= 360.0;
    while (longitude < -180.0) longitude += 360.0;
    return longitude;
  }

  private static double normalizeLatitude(double latitude) {
    return Math.clamp(latitude, -90.0, 90.0);
  }
}
