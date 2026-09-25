package net.yazloysasha.tfcrealworld.util.geography;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.yazloysasha.tfcrealworld.attachment.ModAttachments;
import net.yazloysasha.tfcrealworld.attachment.VisitedWaypoints;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.network.VisitedWaypointUpdatePacket;
import net.yazloysasha.tfcrealworld.util.profile.MapProfile;
import net.yazloysasha.tfcrealworld.util.profile.ProfileManager;
import org.jetbrains.annotations.Nullable;

/**
 * Proximity-based waypoint discovery with flat geography advancements.
 */
public final class WaypointVisitTracker {

  public static final int PROXIMITY_RADIUS = 32;
  private static final double PROXIMITY_RADIUS_SQ =
    (double) PROXIMITY_RADIUS * PROXIMITY_RADIUS;

  private static List<CachedWaypoint> cachedWaypoints = List.of();
  private static @Nullable String cachedProjectionKey;

  private WaypointVisitTracker() {}

  public static void tickPlayer(ServerPlayer player) {
    ensureCache();

    VisitedWaypoints data = player.getData(ModAttachments.VISITED_WAYPOINTS);
    double px = player.getX();
    double pz = player.getZ();
    for (CachedWaypoint waypoint : cachedWaypoints) {
      if (data.isVisited(waypoint.ref())) {
        continue;
      }
      double dx = px - waypoint.x();
      double dz = pz - waypoint.z();
      if (dx * dx + dz * dz <= PROXIMITY_RADIUS_SQ) {
        long at = System.currentTimeMillis();
        if (data.markVisited(waypoint.ref(), at)) {
          PacketDistributor.sendToPlayer(
            player,
            new VisitedWaypointUpdatePacket(waypoint.ref(), at)
          );
          GeographyAdvancements.onWaypointVisited(player, waypoint.ref());
        }
      }
    }
  }

  public static void rebuildCache() {
    cachedProjectionKey = projectionKey();
    MapProfile profile = ProfileManager.getProfile(
      TFCRealWorldConfig.MAP_PROFILE.get()
    );
    List<CachedWaypoint> next = new ArrayList<>();
    for (String waypointRef : profile.waypoints()) {
      GeographyNode node = GeographyManager.get(waypointRef);
      if (
        node == null ||
        node.kind() != GeographyKind.WAYPOINT ||
        node.latitude() == null ||
        node.longitude() == null
      ) {
        continue;
      }
      int[] xz = WaypointCoordinates.toBlockXZ(
        node.latitude(),
        node.longitude()
      );
      next.add(
        new CachedWaypoint(waypointRef.toLowerCase(Locale.ROOT), xz[0], xz[1])
      );
    }
    cachedWaypoints = List.copyOf(next);
  }

  private static void ensureCache() {
    if (
      cachedProjectionKey == null ||
      !cachedProjectionKey.equals(projectionKey())
    ) {
      rebuildCache();
    }
  }

  private static String projectionKey() {
    return (
      TFCRealWorldConfig.MAP_PROFILE.get() +
      "|" +
      TFCRealWorldConfig.HORIZONTAL_SCALE.get() +
      "|" +
      TFCRealWorldConfig.VERTICAL_SCALE.get() +
      "|" +
      TFCRealWorldConfig.getWestEdgeLongitude() +
      "|" +
      TFCRealWorldConfig.getEastEdgeLongitude() +
      "|" +
      TFCRealWorldConfig.getSouthEdgeLatitude() +
      "|" +
      TFCRealWorldConfig.getNorthEdgeLatitude() +
      "|" +
      TFCRealWorldConfig.getMapProjection()
    );
  }

  private record CachedWaypoint(String ref, int x, int z) {}
}
