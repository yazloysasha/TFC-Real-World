package net.yazloysasha.tfcrealworld.util.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.types.CachedGeographicCoords;
import net.yazloysasha.tfcrealworld.types.MapProjection;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;

public record MapProfile(
  String namespace,
  String name,
  int spawnCenterX,
  int spawnCenterZ,
  double westEdgeLongitude,
  double eastEdgeLongitude,
  double southEdgeLatitude,
  double northEdgeLatitude,
  MapProjection mapProjection
) {
  private static final Gson GSON = new GsonBuilder()
    .setPrettyPrinting()
    .create();

  private static final ThreadLocal<
    CachedGeographicCoords
  > GEOGRAPHIC_COORDS_CACHE = new ThreadLocal<>();

  public static MapProfile loadFromResources(String profileId) {
    String lowerProfileId = profileId.toLowerCase();
    String[] parts = ProfileManager.parseProfileId(lowerProfileId);
    String namespaceLower = parts[0];
    String profileNameLower = parts[1];

    // Store in upper-case
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
    return new MapProfile(
      namespace,
      name,
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
  }

  private static MapProfile createDefault(String namespace, String name) {
    return new MapProfile(
      namespace,
      name,
      -9_000,
      -3_000,
      -20.0,
      160.0,
      -90.0,
      90.0,
      MapProjection.EQUAL_EARTH
    );
  }

  private double[] getGeographicCoords() {
    int horizontalTileSize = TFCRealWorldConfig.SPEC != null
      ? TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get()
      : TFCRealWorldConfig.DEFAULT_TILE_SIZE;
    int verticalTileSize = TFCRealWorldConfig.SPEC != null
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
      return new double[] { cached.longitude(), cached.latitude() };
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
}
