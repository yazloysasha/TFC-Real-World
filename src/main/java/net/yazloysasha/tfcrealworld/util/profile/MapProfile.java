package net.yazloysasha.tfcrealworld.util.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.types.CachedGeographicCoords;
import net.yazloysasha.tfcrealworld.types.MapProjection;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;

public record MapProfile(
  String namespace,
  String name,
  int spawnCenterX,
  int spawnCenterZ,
  Integer horizontalTileSize,
  Integer verticalTileSize,
  double westEdgeLongitude,
  double eastEdgeLongitude,
  double southEdgeLatitude,
  double northEdgeLatitude,
  MapProjection mapProjection,
  Map<String, String> lang
) {
  private static final Gson GSON = new GsonBuilder()
    .setPrettyPrinting()
    .create();

  private static final ThreadLocal<
    CachedGeographicCoords
  > GEOGRAPHIC_COORDS_CACHE = new ThreadLocal<>();

  private static final int DEFAULT_SPAWN_CENTER_X = -9_000;
  private static final int DEFAULT_SPAWN_CENTER_Z = -3_000;
  private static final int DEFAULT_HORIZONTAL_TILE_SIZE = 80_000;
  private static final int DEFAULT_VERTICAL_TILE_SIZE = 40_000;
  private static final double DEFAULT_WEST_EDGE_LONGITUDE = -20.0;
  private static final double DEFAULT_EAST_EDGE_LONGITUDE = 160.0;
  private static final double DEFAULT_SOUTH_EDGE_LATITUDE = -90.0;
  private static final double DEFAULT_NORTH_EDGE_LATITUDE = 90.0;
  private static final MapProjection DEFAULT_MAP_PROJECTION =
    MapProjection.EQUAL_EARTH;

  public static MapProfile loadFromResources(String profileId) {
    String lowerProfileId = profileId.toLowerCase();
    String[] parts = ProfileManager.parseProfileId(lowerProfileId);
    String namespaceLower = parts[0];
    String profileNameLower = parts[1];

    String namespace = namespaceLower.toUpperCase();
    String profileName = profileNameLower.toUpperCase();

    ProfileManager.ProfileLocation location = ProfileManager.getProfileLocation(
      profileId
    );

    InputStream stream = null;
    if (location != null) {
      if (location.isZip()) {
        stream = ProfileManager.getSettingsStreamFromZip(
          location.zipPath(),
          namespaceLower,
          profileNameLower
        );
      } else if (location.directoryPath() != null) {
        stream = ProfileManager.getSettingsStreamFromDirectory(
          location.directoryPath()
        );
      }
    }

    if (stream == null) {
      String settingsPath =
        "/data/" +
        TFCRealWorld.MOD_ID +
        "/profiles/" +
        namespaceLower +
        "/" +
        profileNameLower +
        "/settings.json";
      stream = TFCRealWorld.class.getResourceAsStream(settingsPath);
    }

    if (stream == null) {
      TFCRealWorld.LOGGER.error(
        "Profile settings not found for: {}:{}. Using default values.",
        namespace,
        profileName
      );
      return createDefault(namespace, profileName);
    }

    try (InputStream s = stream) {
      JsonObject json = GSON.fromJson(
        new InputStreamReader(s),
        JsonObject.class
      );
      return parseJsonProfile(namespace, profileName, json);
    } catch (Exception e) {
      TFCRealWorld.LOGGER.error(
        "Failed to load profile {}:{}",
        namespace,
        profileName,
        e
      );
      return createDefault(namespace, profileName);
    }
  }

  private static MapProfile parseJsonProfile(
    String namespace,
    String name,
    JsonObject json
  ) {
    Map<String, String> langMap = new HashMap<>();
    if (json.has("lang") && json.get("lang").isJsonObject()) {
      JsonObject langObj = json.get("lang").getAsJsonObject();
      for (String key : langObj.keySet()) {
        langMap.put(key.toLowerCase(), langObj.get(key).getAsString());
      }
    }

    return new MapProfile(
      namespace,
      name,
      json.has("spawn_center_x")
        ? json.get("spawn_center_x").getAsInt()
        : DEFAULT_SPAWN_CENTER_X,
      json.has("spawn_center_z")
        ? json.get("spawn_center_z").getAsInt()
        : DEFAULT_SPAWN_CENTER_Z,
      json.has("horizontal_tile_size")
        ? json.get("horizontal_tile_size").getAsInt()
        : DEFAULT_HORIZONTAL_TILE_SIZE,
      json.has("vertical_tile_size")
        ? json.get("vertical_tile_size").getAsInt()
        : DEFAULT_VERTICAL_TILE_SIZE,
      json.has("west_edge_longitude")
        ? json.get("west_edge_longitude").getAsDouble()
        : DEFAULT_WEST_EDGE_LONGITUDE,
      json.has("east_edge_longitude")
        ? json.get("east_edge_longitude").getAsDouble()
        : DEFAULT_EAST_EDGE_LONGITUDE,
      json.has("south_edge_latitude")
        ? json.get("south_edge_latitude").getAsDouble()
        : DEFAULT_SOUTH_EDGE_LATITUDE,
      json.has("north_edge_latitude")
        ? json.get("north_edge_latitude").getAsDouble()
        : DEFAULT_NORTH_EDGE_LATITUDE,
      json.has("map_projection")
        ? MapProjection.valueOf(
          json.get("map_projection").getAsString().toUpperCase()
        )
        : DEFAULT_MAP_PROJECTION,
      langMap
    );
  }

  private static MapProfile createDefault(String namespace, String name) {
    return new MapProfile(
      namespace,
      name,
      DEFAULT_SPAWN_CENTER_X,
      DEFAULT_SPAWN_CENTER_Z,
      DEFAULT_HORIZONTAL_TILE_SIZE,
      DEFAULT_VERTICAL_TILE_SIZE,
      DEFAULT_WEST_EDGE_LONGITUDE,
      DEFAULT_EAST_EDGE_LONGITUDE,
      DEFAULT_SOUTH_EDGE_LATITUDE,
      DEFAULT_NORTH_EDGE_LATITUDE,
      DEFAULT_MAP_PROJECTION,
      new HashMap<>()
    );
  }

  private double[] getGeographicCoords() {
    CachedGeographicCoords cached = GEOGRAPHIC_COORDS_CACHE.get();
    if (
      cached != null &&
      cached.spawnCenterX() == spawnCenterX &&
      cached.spawnCenterZ() == spawnCenterZ &&
      cached.horizontalTileSize() == this.horizontalTileSize &&
      cached.verticalTileSize() == this.verticalTileSize
    ) {
      return new double[] { cached.longitude(), cached.latitude() };
    }

    double[] geoCoords = ProjectionManager.minecraftToGeographic(
      spawnCenterX,
      spawnCenterZ,
      this.horizontalTileSize,
      this.verticalTileSize,
      westEdgeLongitude,
      eastEdgeLongitude,
      southEdgeLatitude,
      northEdgeLatitude,
      mapProjection
    );

    GEOGRAPHIC_COORDS_CACHE.set(
      new CachedGeographicCoords(
        spawnCenterX,
        spawnCenterZ,
        horizontalTileSize,
        verticalTileSize,
        geoCoords[0],
        geoCoords[1]
      )
    );

    return geoCoords;
  }

  public double getSpawnCenterLongitude() {
    return getGeographicCoords()[0];
  }

  public double getSpawnCenterLatitude() {
    return getGeographicCoords()[1];
  }

  public String getDisplayName(String languageCode) {
    if (lang == null || lang.isEmpty()) {
      return namespace + ":" + name;
    }

    String langKey = languageCode != null
      ? languageCode.toLowerCase()
      : "en_us";
    return lang.getOrDefault(
      langKey,
      lang.getOrDefault("en_us", namespace + ":" + name)
    );
  }
}
