package net.yazloysasha.tfcrealworld.util.projection;

import java.util.HashMap;
import java.util.Map;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.types.MapProjection;

public class ProjectionManager {

  private static final Map<MapProjection, MapProjectionStrategy> strategies =
    new HashMap<>();

  static {
    strategies.put(
      MapProjection.EQUAL_EARTH,
      new EqualEarthProjectionStrategy()
    );
  }

  public static MapProjectionStrategy getStrategy(MapProjection projection) {
    MapProjectionStrategy strategy = strategies.get(projection);
    if (strategy == null) {
      throw new IllegalArgumentException("Unknown projection: " + projection);
    }
    return strategy;
  }

  private static double normalizeLongitude(double longitude) {
    return longitude % 360.0;
  }

  public static double getLatitudeByZ(double z) {
    MapProjection projection = TFCRealWorldConfig.getMapProjection();
    MapProjectionStrategy strategy = getStrategy(projection);

    double west = normalizeLongitude(TFCRealWorldConfig.getWestEdgeLongitude());
    double east = normalizeLongitude(TFCRealWorldConfig.getEastEdgeLongitude());
    double tileCenterLongitude = (west + east) / 2.0;

    return strategy.getLatitudeByZ(
      z,
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.get(),
      TFCRealWorldConfig.getSouthEdgeLatitude(),
      TFCRealWorldConfig.getNorthEdgeLatitude(),
      tileCenterLongitude
    );
  }

  public static double[] geographicToClassic(
    double longitude,
    double latitude,
    int horizontalTileSize,
    int verticalTileSize,
    double westEdgeLongitude,
    double eastEdgeLongitude,
    double southEdgeLatitude,
    double northEdgeLatitude,
    MapProjection projection
  ) {
    MapProjectionStrategy strategy = getStrategy(projection);

    longitude = normalizeLongitude(longitude);
    latitude = Math.clamp(latitude, -90.0, 90.0);

    double west = normalizeLongitude(westEdgeLongitude);
    double east = normalizeLongitude(eastEdgeLongitude);

    double tileCenterLongitude = (west + east) / 2.0;
    double tileCenterLatitude = (southEdgeLatitude + northEdgeLatitude) / 2.0;

    return strategy.geographicToClassic(
      longitude,
      latitude,
      horizontalTileSize,
      verticalTileSize,
      west,
      east,
      southEdgeLatitude,
      northEdgeLatitude,
      tileCenterLongitude,
      tileCenterLatitude
    );
  }

  public static double[] classicToGeographic(
    double x,
    double z,
    int horizontalTileSize,
    int verticalTileSize,
    double westEdgeLongitude,
    double eastEdgeLongitude,
    double southEdgeLatitude,
    double northEdgeLatitude,
    MapProjection projection
  ) {
    MapProjectionStrategy strategy = getStrategy(projection);

    double west = normalizeLongitude(westEdgeLongitude);
    double east = normalizeLongitude(eastEdgeLongitude);

    double tileCenterLongitude = (west + east) / 2.0;
    double tileCenterLatitude = (southEdgeLatitude + northEdgeLatitude) / 2.0;

    double[] result = strategy.classicToGeographic(
      x,
      z,
      horizontalTileSize,
      verticalTileSize,
      west,
      east,
      southEdgeLatitude,
      northEdgeLatitude,
      tileCenterLongitude,
      tileCenterLatitude
    );

    result[0] = normalizeLongitude(result[0]);
    return result;
  }

  public static double[] geographicToClassic(
    double longitude,
    double latitude
  ) {
    return geographicToClassic(
      longitude,
      latitude,
      TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get(),
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.get(),
      TFCRealWorldConfig.getWestEdgeLongitude(),
      TFCRealWorldConfig.getEastEdgeLongitude(),
      TFCRealWorldConfig.getSouthEdgeLatitude(),
      TFCRealWorldConfig.getNorthEdgeLatitude(),
      TFCRealWorldConfig.getMapProjection()
    );
  }

  public static double[] classicToGeographic(double x, double z) {
    return classicToGeographic(
      x,
      z,
      TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get(),
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.get(),
      TFCRealWorldConfig.getWestEdgeLongitude(),
      TFCRealWorldConfig.getEastEdgeLongitude(),
      TFCRealWorldConfig.getSouthEdgeLatitude(),
      TFCRealWorldConfig.getNorthEdgeLatitude(),
      TFCRealWorldConfig.getMapProjection()
    );
  }
}
