package net.yazloysasha.tfcrealworld.util.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.types.CachedClassicCoords;
import net.yazloysasha.tfcrealworld.types.MapProjection;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;

public record MapProfile(
  String namespace,
  String name,
  int index,
  double spawnCenterLongitude,
  double spawnCenterLatitude,
  int horizontalScale,
  int verticalScale,
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

  private static final ThreadLocal<CachedClassicCoords> CLASSIC_COORDS_CACHE =
    new ThreadLocal<>();

  private static final Integer DEFAULT_INDEX = Integer.MAX_VALUE;
  private static final double DEFAULT_SPAWN_CENTER_LONGITUDE = 12.4964;
  private static final double DEFAULT_SPAWN_CENTER_LATITUDE = 41.9028;
  private static final int DEFAULT_HORIZONTAL_SCALE = 40_000;
  private static final int DEFAULT_VERTICAL_SCALE = 20_000;
  private static final double DEFAULT_WEST_EDGE_LONGITUDE = -170.0;
  private static final double DEFAULT_EAST_EDGE_LONGITUDE = 190.0;
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
      json.has("index") ? json.get("index").getAsInt() : DEFAULT_INDEX,
      json.has("spawn_center_longitude")
        ? json.get("spawn_center_longitude").getAsDouble()
        : DEFAULT_SPAWN_CENTER_LONGITUDE,
      json.has("spawn_center_latitude")
        ? json.get("spawn_center_latitude").getAsDouble()
        : DEFAULT_SPAWN_CENTER_LATITUDE,
      json.has("horizontal_scale")
        ? json.get("horizontal_scale").getAsInt()
        : json.has("horizontal_tile_size")
          ? json.get("horizontal_tile_size").getAsInt() / 2
          : DEFAULT_HORIZONTAL_SCALE,
      json.has("vertical_scale")
        ? json.get("vertical_scale").getAsInt()
        : json.has("vertical_tile_size")
          ? json.get("vertical_tile_size").getAsInt() / 2
          : DEFAULT_VERTICAL_SCALE,
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
      DEFAULT_INDEX,
      DEFAULT_SPAWN_CENTER_LONGITUDE,
      DEFAULT_SPAWN_CENTER_LATITUDE,
      DEFAULT_HORIZONTAL_SCALE,
      DEFAULT_VERTICAL_SCALE,
      DEFAULT_WEST_EDGE_LONGITUDE,
      DEFAULT_EAST_EDGE_LONGITUDE,
      DEFAULT_SOUTH_EDGE_LATITUDE,
      DEFAULT_NORTH_EDGE_LATITUDE,
      DEFAULT_MAP_PROJECTION,
      new HashMap<>()
    );
  }

  private int[] getClassicCoords() {
    CachedClassicCoords cached = CLASSIC_COORDS_CACHE.get();
    if (
      cached != null &&
      cached.spawnCenterLongitude() == spawnCenterLongitude &&
      cached.spawnCenterLatitude() == spawnCenterLatitude &&
      cached.horizontalScale() == this.horizontalScale &&
      cached.verticalScale() == this.verticalScale
    ) {
      return new int[] { cached.spawnCenterX(), cached.spawnCenterZ() };
    }

    double[] classicCoords = ProjectionManager.geographicToClassic(
      spawnCenterLongitude,
      spawnCenterLatitude,
      this.horizontalScale,
      this.verticalScale,
      westEdgeLongitude,
      eastEdgeLongitude,
      southEdgeLatitude,
      northEdgeLatitude,
      mapProjection
    );

    int spawnCenterX = (int) Math.round(classicCoords[0]);
    int spawnCenterZ = (int) Math.round(classicCoords[1]);

    CLASSIC_COORDS_CACHE.set(
      new CachedClassicCoords(
        spawnCenterLongitude,
        spawnCenterLatitude,
        spawnCenterX,
        spawnCenterZ,
        horizontalScale,
        verticalScale
      )
    );

    return new int[] { spawnCenterX, spawnCenterZ };
  }

  public int getSpawnCenterX() {
    return getClassicCoords()[0];
  }

  public int getSpawnCenterZ() {
    return getClassicCoords()[1];
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
