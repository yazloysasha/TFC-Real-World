package net.yazloysasha.tfcrealworld.util.geography;

import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;

/**
 * Converts geographic waypoint coordinates to block X/Z for the active map
 * profile. Longitudes are shifted by multiples of 360° into the profile's
 * edge window when possible (new/old-world corner zones); remaining off-map
 * projections are clamped to the map radius so they sit on the visible edge.
 */
public final class WaypointCoordinates {

  private WaypointCoordinates() {}

  public static int[] toBlockXZ(double latitude, double longitude) {
    double west = TFCRealWorldConfig.getWestEdgeLongitude();
    double east = TFCRealWorldConfig.getEastEdgeLongitude();
    double fittedLongitude = fitLongitude(longitude, west, east);

    double[] classic = ProjectionManager.geographicToClassic(
      fittedLongitude,
      latitude
    );

    int horizontalScale = TFCRealWorldConfig.HORIZONTAL_SCALE.get();
    int verticalScale = TFCRealWorldConfig.VERTICAL_SCALE.get();

    int x = (int) Math.round(
      Math.clamp(classic[0], -horizontalScale, horizontalScale)
    );
    int z = (int) Math.round(
      Math.clamp(classic[1], -verticalScale, verticalScale)
    );
    return new int[] { x, z };
  }

  static double fitLongitude(double longitude, double west, double east) {
    double mid = (west + east) / 2.0;
    Double bestInRange = null;
    double bestInRangeDist = Double.POSITIVE_INFINITY;

    for (int k = -2; k <= 2; k++) {
      double candidate = longitude + 360.0 * k;
      if (candidate >= west && candidate <= east) {
        double dist = Math.abs(candidate - mid);
        if (dist < bestInRangeDist) {
          bestInRangeDist = dist;
          bestInRange = candidate;
        }
      }
    }

    return bestInRange != null ? bestInRange : longitude;
  }
}
