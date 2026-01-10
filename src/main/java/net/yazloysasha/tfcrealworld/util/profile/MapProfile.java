package net.yazloysasha.tfcrealworld.util.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.types.MapProjection;
import org.slf4j.Logger;

public record MapProfile(
  String id,
  double spawnCenterLongitude,
  double spawnCenterLatitude,
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

  public static MapProfile loadFromResources(String profileId) {
    String settingsPath =
      "/data/" +
      TFCRealWorld.MOD_ID +
      "/profiles/" +
      profileId +
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
        json.get("spawn_center_longitude").getAsDouble(),
        json.get("spawn_center_latitude").getAsDouble(),
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
      29.216615,
      9.779201,
      -9_000,
      -3_000,
      -20.0,
      160.0,
      -90.0,
      90.0,
      MapProjection.EQUAL_EARTH
    );
  }
}
