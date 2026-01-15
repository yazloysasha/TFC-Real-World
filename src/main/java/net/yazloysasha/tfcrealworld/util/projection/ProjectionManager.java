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

  private static double calculateTileCenterLongitude(
    double westEdgeLongitude,
    double eastEdgeLongitude
  ) {
    if (westEdgeLongitude > eastEdgeLongitude) {
      westEdgeLongitude -= 360.0;
    }

    return (westEdgeLongitude + eastEdgeLongitude) / 2.0;
  }

  public static double getLatitudeByZ(double z) {
    MapProjection projection = TFCRealWorldConfig.getMapProjection();
    MapProjectionStrategy strategy = getStrategy(projection);

    double westEdgeLongitude = TFCRealWorldConfig.getWestEdgeLongitude();
    double eastEdgeLongitude = TFCRealWorldConfig.getEastEdgeLongitude();

    double tileCenterLongitude = calculateTileCenterLongitude(
      westEdgeLongitude,
      eastEdgeLongitude
    );

    return strategy.getLatitudeByZ(
      z,
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.get(),
      TFCRealWorldConfig.getSouthEdgeLatitude(),
      TFCRealWorldConfig.getNorthEdgeLatitude(),
      tileCenterLongitude
    );
  }

  public static double[] geographicToMinecraft(
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

    double tileCenterLongitude = calculateTileCenterLongitude(
      westEdgeLongitude,
      eastEdgeLongitude
    );
    double tileCenterLatitude = (southEdgeLatitude + northEdgeLatitude) / 2.0;

    return strategy.geographicToMinecraft(
      longitude,
      latitude,
      horizontalTileSize,
      verticalTileSize,
      westEdgeLongitude,
      eastEdgeLongitude,
      southEdgeLatitude,
      northEdgeLatitude,
      tileCenterLongitude,
      tileCenterLatitude
    );
  }

  public static double[] minecraftToGeographic(
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

    double tileCenterLongitude = calculateTileCenterLongitude(
      westEdgeLongitude,
      eastEdgeLongitude
    );
    double tileCenterLatitude = (southEdgeLatitude + northEdgeLatitude) / 2.0;

    return strategy.minecraftToGeographic(
      x,
      z,
      horizontalTileSize,
      verticalTileSize,
      westEdgeLongitude,
      eastEdgeLongitude,
      southEdgeLatitude,
      northEdgeLatitude,
      tileCenterLongitude,
      tileCenterLatitude
    );
  }

  public static double[] geographicToMinecraft(
    double longitude,
    double latitude
  ) {
    MapProjection projection = TFCRealWorldConfig.getMapProjection();
    MapProjectionStrategy strategy = getStrategy(projection);

    double westEdgeLongitude = TFCRealWorldConfig.getWestEdgeLongitude();
    double eastEdgeLongitude = TFCRealWorldConfig.getEastEdgeLongitude();
    double southEdgeLatitude = TFCRealWorldConfig.getSouthEdgeLatitude();
    double northEdgeLatitude = TFCRealWorldConfig.getNorthEdgeLatitude();

    double tileCenterLongitude = calculateTileCenterLongitude(
      westEdgeLongitude,
      eastEdgeLongitude
    );
    double tileCenterLatitude = (southEdgeLatitude + northEdgeLatitude) / 2.0;

    return strategy.geographicToMinecraft(
      longitude,
      latitude,
      TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get(),
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.get(),
      westEdgeLongitude,
      eastEdgeLongitude,
      southEdgeLatitude,
      northEdgeLatitude,
      tileCenterLongitude,
      tileCenterLatitude
    );
  }

  public static double[] minecraftToGeographic(double x, double z) {
    MapProjection projection = TFCRealWorldConfig.getMapProjection();
    MapProjectionStrategy strategy = getStrategy(projection);

    double westEdgeLongitude = TFCRealWorldConfig.getWestEdgeLongitude();
    double eastEdgeLongitude = TFCRealWorldConfig.getEastEdgeLongitude();
    double southEdgeLatitude = TFCRealWorldConfig.getSouthEdgeLatitude();
    double northEdgeLatitude = TFCRealWorldConfig.getNorthEdgeLatitude();

    double tileCenterLongitude = calculateTileCenterLongitude(
      westEdgeLongitude,
      eastEdgeLongitude
    );
    double tileCenterLatitude = (southEdgeLatitude + northEdgeLatitude) / 2.0;

    return strategy.minecraftToGeographic(
      x,
      z,
      TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get(),
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.get(),
      westEdgeLongitude,
      eastEdgeLongitude,
      southEdgeLatitude,
      northEdgeLatitude,
      tileCenterLongitude,
      tileCenterLatitude
    );
  }
}
