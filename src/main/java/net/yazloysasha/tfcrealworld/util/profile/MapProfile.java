package net.yazloysasha.tfcrealworld.util.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.types.CachedGeographicCoords;
import net.yazloysasha.tfcrealworld.types.MapProjection;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;
import org.slf4j.Logger;

public record MapProfile(
  String id,
  int spawnCenterX,
  int spawnCenterZ,
  double westEdgeLongitude,
  double eastEdgeLongitude,
  double southEdgeLatitude,
  double northEdgeLatitude,
  MapProjection mapProjection
) {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Gson GSON = new GsonBuilder()
    .setPrettyPrinting()
    .create();

  private static final ThreadLocal<
    CachedGeographicCoords
  > GEOGRAPHIC_COORDS_CACHE = new ThreadLocal<>();

  public static MapProfile loadFromResources(String profileId) {
    String settingsPath =
      "/data/" +
      TFCRealWorld.MOD_ID +
      "/profiles/" +
      profileId.toLowerCase() +
      "/settings.json";

    try (
      InputStream stream = TFCRealWorld.class.getResourceAsStream(settingsPath)
    ) {
      if (stream == null) {
        LOGGER.error(
          "Profile settings not found at: {}. Using default values.",
          settingsPath
        );
        return createDefault(profileId);
      }

      JsonObject json = GSON.fromJson(
        new InputStreamReader(stream),
        JsonObject.class
      );

      return new MapProfile(
        profileId,
        json.get("spawn_center_x").getAsInt(),
        json.get("spawn_center_z").getAsInt(),
        json.get("west_edge_longitude").getAsDouble(),
        json.get("east_edge_longitude").getAsDouble(),
        json.get("south_edge_latitude").getAsDouble(),
        json.get("north_edge_latitude").getAsDouble(),
        MapProjection.valueOf(
          json.get("map_projection").getAsString().toUpperCase()
        )
      );
    } catch (Exception e) {
      LOGGER.error("Failed to load profile {} from resources", profileId, e);
      return createDefault(profileId);
    }
  }

  private static MapProfile createDefault(String profileId) {
    return new MapProfile(
      profileId,
      -9_000,
      -3_000,
      -20.0,
      160.0,
      -90.0,
      90.0,
      MapProjection.EQUAL_EARTH
    );
  }

  public double getSpawnCenterLongitude() {
    int horizontalTileSize = TFCRealWorldConfig.SPEC != null
      ? TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get()
      : TFCRealWorldConfig.DEFAULT_TILE_SIZE;
    int verticalTileSize = TFCRealWorldConfig.VERTICAL_TILE_SIZE != null
      ? TFCRealWorldConfig.VERTICAL_TILE_SIZE.get()
      : TFCRealWorldConfig.DEFAULT_TILE_SIZE;

    CachedGeographicCoords cached = GEOGRAPHIC_COORDS_CACHE.get();
    if (
      cached != null &&
      cached.spawnCenterX() == spawnCenterX &&
      cached.spawnCenterZ() == spawnCenterZ &&
      cached.horizontalTileSize() == horizontalTileSize &&
      cached.verticalTileSize() == verticalTileSize
    ) {
      return cached.longitude();
    }

    double[] geoCoords = ProjectionManager.minecraftToGeographic(
      spawnCenterX,
      spawnCenterZ,
      horizontalTileSize,
      verticalTileSize,
      westEdgeLongitude,
      eastEdgeLongitude,
      southEdgeLatitude,
      northEdgeLatitude,
      mapProjection
    );

    CachedGeographicCoords newCache = new CachedGeographicCoords(
      spawnCenterX,
      spawnCenterZ,
      horizontalTileSize,
      verticalTileSize,
      geoCoords[0],
      geoCoords[1]
    );
    GEOGRAPHIC_COORDS_CACHE.set(newCache);

    return geoCoords[0];
  }

  public double getSpawnCenterLatitude() {
    int horizontalTileSize = TFCRealWorldConfig.SPEC != null
      ? TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get()
      : TFCRealWorldConfig.DEFAULT_TILE_SIZE;
    int verticalTileSize = TFCRealWorldConfig.VERTICAL_TILE_SIZE != null
      ? TFCRealWorldConfig.VERTICAL_TILE_SIZE.get()
      : TFCRealWorldConfig.DEFAULT_TILE_SIZE;

    CachedGeographicCoords cached = GEOGRAPHIC_COORDS_CACHE.get();
    if (
      cached != null &&
      cached.spawnCenterX() == spawnCenterX &&
      cached.spawnCenterZ() == spawnCenterZ &&
      cached.horizontalTileSize() == horizontalTileSize &&
      cached.verticalTileSize() == verticalTileSize
    ) {
      return cached.latitude();
    }

    double[] geoCoords = ProjectionManager.minecraftToGeographic(
      spawnCenterX,
      spawnCenterZ,
      horizontalTileSize,
      verticalTileSize,
      westEdgeLongitude,
      eastEdgeLongitude,
      southEdgeLatitude,
      northEdgeLatitude,
      mapProjection
    );

    CachedGeographicCoords newCache = new CachedGeographicCoords(
      spawnCenterX,
      spawnCenterZ,
      horizontalTileSize,
      verticalTileSize,
      geoCoords[0],
      geoCoords[1]
    );
    GEOGRAPHIC_COORDS_CACHE.set(newCache);

    return geoCoords[1];
  }
}
