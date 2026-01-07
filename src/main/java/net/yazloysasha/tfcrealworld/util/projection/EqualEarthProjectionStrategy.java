package net.yazloysasha.tfcrealworld.util.projection;

/**
 * TODO: Исправить расчёт координат, сейчас он неверный
 * Использовать реальную формулу для проекции Equal Earth, как в data/maps.py
 */
public class EqualEarthProjectionStrategy implements MapProjectionStrategy {

  private static final double SQRT_3 = Math.sqrt(3.0);

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

    double centralLonRad = Math.toRadians(tileCenterLongitude);
    double lonRad = Math.toRadians(spawnCenterLongtitude);
    double latRad = Math.toRadians(spawnCenterLatitude);
    double deltaLon = lonRad - centralLonRad;

    while (deltaLon > Math.PI) deltaLon -= 2.0 * Math.PI;
    while (deltaLon < -Math.PI) deltaLon += 2.0 * Math.PI;

    double sinLat = Math.sin(latRad);
    double sinLatSq = sinLat * sinLat;
    double denominator = 1.0 + 3.0 * sinLatSq;

    double projX = (deltaLon * Math.cos(latRad)) / denominator;

    double westLonRad = Math.toRadians(westEdgeLongitude);
    double eastLonRad = Math.toRadians(eastEdgeLongitude);

    double deltaWestLon = westLonRad - centralLonRad;
    double deltaEastLon = eastLonRad - centralLonRad;

    while (deltaWestLon > Math.PI) deltaWestLon -= 2.0 * Math.PI;
    while (deltaWestLon < -Math.PI) deltaWestLon += 2.0 * Math.PI;
    while (deltaEastLon > Math.PI) deltaEastLon -= 2.0 * Math.PI;
    while (deltaEastLon < -Math.PI) deltaEastLon += 2.0 * Math.PI;

    double centralLatRad = Math.toRadians(tileCenterLatitude);
    double sinCentralLat = Math.sin(centralLatRad);
    double sinCentralLatSq = sinCentralLat * sinCentralLat;
    double centralDenominator = 1.0 + 3.0 * sinCentralLatSq;

    double westProjX =
      (deltaWestLon * Math.cos(centralLatRad)) / centralDenominator;
    double eastProjX =
      (deltaEastLon * Math.cos(centralLatRad)) / centralDenominator;

    double projWidth = Math.abs(eastProjX - westProjX);

    double normalizedX = (projX - westProjX) / projWidth;
    normalizedX = Math.clamp(normalizedX, 0.0, 1.0);

    int x = (int) Math.round(
      normalizedX * horizontalTileSize - horizontalTileSize / 2.0
    );

    double southLatRad = Math.toRadians(southEdgeLatitude);
    double northLatRad = Math.toRadians(northEdgeLatitude);

    double sinSouthLat = Math.sin(southLatRad);
    double sinNorthLat = Math.sin(northLatRad);
    double southDenominator = 1.0 + 3.0 * sinSouthLat * sinSouthLat;
    double northDenominator = 1.0 + 3.0 * sinNorthLat * sinNorthLat;

    double projY = (SQRT_3 * sinLat) / denominator;
    double southProjY = (SQRT_3 * sinSouthLat) / southDenominator;
    double northProjY = (SQRT_3 * sinNorthLat) / northDenominator;

    double projHeight = Math.abs(northProjY - southProjY);

    double normalizedY = (projY - southProjY) / projHeight;
    normalizedY = Math.clamp(normalizedY, 0.0, 1.0);

    int z = (int) Math.round(
      normalizedY * verticalTileSize - verticalTileSize / 2.0
    );

    return new int[] { x, z };
  }

  private static double normalizeLongitude(double longitude) {
    while (longitude > 180.0) longitude -= 360.0;
    while (longitude < -180.0) longitude += 360.0;
    return longitude;
  }
}
