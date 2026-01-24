package net.yazloysasha.tfcrealworld.util.projection;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
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
      TFCRealWorldConfig.VERTICAL_SCALE.get(),
      TFCRealWorldConfig.getSouthEdgeLatitude(),
      TFCRealWorldConfig.getNorthEdgeLatitude(),
      tileCenterLongitude
    );
  }

  public static double[] geographicToClassic(
    double longitude,
    double latitude,
    int horizontalScale,
    int verticalScale,
    double westEdgeLongitude,
    double eastEdgeLongitude,
    double southEdgeLatitude,
    double northEdgeLatitude,
    MapProjection projection
  ) {
    MapProjectionStrategy strategy = getStrategy(projection);

    longitude = normalizeLongitude(longitude);
    latitude = Mth.clamp(latitude, -90.0, 90.0);

    double west = normalizeLongitude(westEdgeLongitude);
    double east = normalizeLongitude(eastEdgeLongitude);

    double tileCenterLongitude = (west + east) / 2.0;
    double tileCenterLatitude = (southEdgeLatitude + northEdgeLatitude) / 2.0;

    return strategy.geographicToClassic(
      longitude,
      latitude,
      horizontalScale,
      verticalScale,
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
    int horizontalScale,
    int verticalScale,
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

    double tileRadiusBlocksX = horizontalScale / 2.0;
    double tileRadiusBlocksZ = verticalScale / 2.0;
    double tileRadiusGridX =
      tileRadiusBlocksX / TFCRealWorld.GRID_WIDTH_IN_BLOCK;
    double tileRadiusGridZ =
      tileRadiusBlocksZ / TFCRealWorld.GRID_WIDTH_IN_BLOCK;

    double gridX = x / TFCRealWorld.GRID_WIDTH_IN_BLOCK;
    double gridZ = z / TFCRealWorld.GRID_WIDTH_IN_BLOCK;

    int tileX = (int) Math.floor(
      (gridX + tileRadiusGridX) / (2.0 * tileRadiusGridX)
    );
    int tileZ = (int) Math.floor(
      (gridZ + tileRadiusGridZ) / (2.0 * tileRadiusGridZ)
    );

    double tileCenterX = tileX * 2.0 * tileRadiusGridX;
    double tileCenterZ = tileZ * 2.0 * tileRadiusGridZ;
    double localX = gridX - tileCenterX;
    double localZ = gridZ - tileCenterZ;

    if (Math.floorMod(tileX, 2) != 0) {
      localX = -localX;
    }
    if (Math.floorMod(tileZ, 2) != 0) {
      localZ = -localZ;
    }

    double unreflectedX = localX * TFCRealWorld.GRID_WIDTH_IN_BLOCK;
    double unreflectedZ = localZ * TFCRealWorld.GRID_WIDTH_IN_BLOCK;

    double[] result = strategy.classicToGeographic(
      unreflectedX,
      unreflectedZ,
      horizontalScale,
      verticalScale,
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
      TFCRealWorldConfig.HORIZONTAL_SCALE.get(),
      TFCRealWorldConfig.VERTICAL_SCALE.get(),
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
      TFCRealWorldConfig.HORIZONTAL_SCALE.get(),
      TFCRealWorldConfig.VERTICAL_SCALE.get(),
      TFCRealWorldConfig.getWestEdgeLongitude(),
      TFCRealWorldConfig.getEastEdgeLongitude(),
      TFCRealWorldConfig.getSouthEdgeLatitude(),
      TFCRealWorldConfig.getNorthEdgeLatitude(),
      TFCRealWorldConfig.getMapProjection()
    );
  }
}
