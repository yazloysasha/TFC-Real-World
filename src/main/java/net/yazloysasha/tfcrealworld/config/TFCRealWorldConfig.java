package net.yazloysasha.tfcrealworld.config;

import java.util.Arrays;
import java.util.List;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.types.MapProjection;
import net.yazloysasha.tfcrealworld.types.SpawnMode;
import net.yazloysasha.tfcrealworld.util.profile.MapProfile;
import net.yazloysasha.tfcrealworld.util.profile.ProfileManager;

public class TFCRealWorldConfig {

  public static final ModConfigSpec.Builder BUILDER =
    new ModConfigSpec.Builder();
  public static final ModConfigSpec SPEC;

  private static ModConfig modConfig;

  public static final int MIN_SCALE = 256;
  public static final int MAX_SCALE = 100_000;
  public static final int DEFAULT_SCALE = 20_000;
  public static final String DEFAULT_MAP_PROFILE = "DEFAULT:FULL_EQUAL_EARTH";

  public static final ConfigOption<String> MAP_PROFILE;
  public static final ConfigOption<SpawnMode> SPAWN_MODE;
  public static final ConfigOption<Double> SPAWN_CENTER_LONGITUDE;
  public static final ConfigOption<Double> SPAWN_CENTER_LATITUDE;
  public static final ConfigOption<Integer> SPAWN_CENTER_X;
  public static final ConfigOption<Integer> SPAWN_CENTER_Z;
  public static final ConfigOption<Integer> SPAWN_DISTANCE;
  public static final ConfigOption<Boolean> CANYONS_NOT_VOLCANIC;
  public static final ConfigOption<Boolean> FLAT_BEDROCK;
  public static final ConfigOption<Boolean> FINITE_CONTINENTS;
  public static final ConfigOption<Double> CONTINENTALNESS;
  public static final ConfigOption<Double> GRASS_DENSITY;
  public static final ConfigOption<Double> TEMPERATURE_CONSTANT;
  public static final ConfigOption<Double> RAINFALL_CONSTANT;
  public static final ConfigOption<Integer> TEMPERATURE_SCALE;
  public static final ConfigOption<Integer> RAINFALL_SCALE;
  public static final ConfigOption<Integer> HORIZONTAL_SCALE;
  public static final ConfigOption<Integer> VERTICAL_SCALE;
  public static final ConfigOption<Boolean> CONTINENT_FROM_MAP;
  public static final ConfigOption<Boolean> ALTITUDE_FROM_MAP;
  public static final ConfigOption<Boolean> HOTSPOTS_FROM_MAP;
  public static final ConfigOption<Boolean> KOPPEN_FROM_MAP;

  private static final List<ConfigOption<?>> allOptions;

  static {
    BUILDER.comment("TFC: Real World Configuration");
    BUILDER.push("map_settings");

    MAP_PROFILE = new ConfigOption<>(
      BUILDER,
      "map_profile",
      "Map profile ID. Profiles are loaded from config/" +
      TFCRealWorld.MOD_ID +
      "/profiles/<namespace>/<id>/**",
      DEFAULT_MAP_PROFILE
    );

    BUILDER.pop();
    BUILDER.push("spawn_settings");

    SPAWN_MODE = new ConfigOption<>(
      BUILDER,
      "spawn_mode",
      "Spawn location mode: GEOGRAPHIC (use longitude/latitude), RANDOM (from seed), or CLASSIC (use coordinates)",
      SpawnMode.GEOGRAPHIC,
      SpawnMode.class
    );
    SPAWN_CENTER_LONGITUDE = new ConfigOption<>(
      BUILDER,
      "spawn_center_longitude",
      "Geographic longitude for the center position around which the spawn position is chosen (used in GEOGRAPHIC mode)",
      getSpawnCenterLongitude(),
      -360.0,
      360.0
    );
    SPAWN_CENTER_LATITUDE = new ConfigOption<>(
      BUILDER,
      "spawn_center_latitude",
      "Geographic latitude for the center position around which the spawn position is chosen (used in GEOGRAPHIC mode)",
      getSpawnCenterLatitude(),
      -90.0,
      90.0
    );

    BUILDER.pop();
    BUILDER.push("tfc_spawn_settings");

    SPAWN_CENTER_X = new ConfigOption<>(
      BUILDER,
      "spawn_center_x",
      "TFC option. The center X position around which the spawn position is chosen (used in CLASSIC mode)",
      getSpawnCenterX(),
      -MAX_SCALE,
      MAX_SCALE
    );
    SPAWN_CENTER_Z = new ConfigOption<>(
      BUILDER,
      "spawn_center_z",
      "TFC option. The center Z position around which the spawn position is chosen (used in CLASSIC mode)",
      getSpawnCenterZ(),
      -MAX_SCALE,
      MAX_SCALE
    );
    SPAWN_DISTANCE = new ConfigOption<>(
      BUILDER,
      "spawn_distance",
      "TFC option. The maximum distance from the spawn center the spawn position will be chosen",
      MIN_SCALE,
      MIN_SCALE,
      MAX_SCALE
    );

    BUILDER.pop();
    BUILDER.push("biome_modifications");

    CANYONS_NOT_VOLCANIC = new ConfigOption<>(
      BUILDER,
      "canyons_not_volcanic",
      "Remove volcanic features from Canyons and Doline Canyons biomes",
      true
    );

    BUILDER.pop();
    BUILDER.push("world_generation");

    FLAT_BEDROCK = new ConfigOption<>(
      BUILDER,
      "flat_bedrock",
      "TFC option. If the bottom of the world is a single layer of flat bedrock, or random like vanilla",
      false
    );
    FINITE_CONTINENTS = new ConfigOption<>(
      BUILDER,
      "finite_continents",
      "TFC option. If the world should generate only a few continents, leaving a vast ocean beyond",
      false
    );
    CONTINENTALNESS = new ConfigOption<>(
      BUILDER,
      "continentalness",
      "TFC option. Determines the nature of continents. Smaller values create smaller continents and more islands, larger values create larger, blobbier continents",
      0.5,
      0.0,
      1.0
    );
    GRASS_DENSITY = new ConfigOption<>(
      BUILDER,
      "grass_density",
      "TFC option. Affects how much grass generates in the world. Higher values indicate more grass coverage",
      0.5,
      0.0,
      1.0
    );
    TEMPERATURE_CONSTANT = new ConfigOption<>(
      BUILDER,
      "temperature_constant",
      "TFC option. A number representing the temperature for an entire world, where -1.0 is polar and 1.0 is tropical",
      0.0,
      -1.0,
      1.0
    );
    RAINFALL_CONSTANT = new ConfigOption<>(
      BUILDER,
      "rainfall_constant",
      "TFC option. A number representing the rainfall for an entire world, where -1.0 is arid and 1.0 is tropical",
      0.0,
      -1.0,
      1.0
    );
    TEMPERATURE_SCALE = new ConfigOption<>(
      BUILDER,
      "temperature_scale",
      "TFC option. The distance between two temperature extremes, in blocks",
      DEFAULT_SCALE,
      MIN_SCALE,
      MAX_SCALE
    );
    RAINFALL_SCALE = new ConfigOption<>(
      BUILDER,
      "rainfall_scale",
      "TFC option. The distance between two rainfall extremes, in blocks",
      DEFAULT_SCALE,
      MIN_SCALE,
      MAX_SCALE
    );

    BUILDER.pop();
    BUILDER.push("generation_modes");

    HORIZONTAL_SCALE = new ConfigOption<>(
      BUILDER,
      "horizontal_scale",
      "Horizontal map radius in blocks",
      getHorizontalScale(),
      MIN_SCALE,
      MAX_SCALE
    );
    VERTICAL_SCALE = new ConfigOption<>(
      BUILDER,
      "vertical_scale",
      "Vertical map radius in blocks",
      getVerticalScale(),
      MIN_SCALE,
      MAX_SCALE
    );
    CONTINENT_FROM_MAP = new ConfigOption<>(
      BUILDER,
      "continent_from_map",
      "Generate continents from map or procedurally",
      true
    );
    ALTITUDE_FROM_MAP = new ConfigOption<>(
      BUILDER,
      "altitude_from_map",
      "Generate base land height and ocean depth from altitude map or procedurally",
      true
    );
    HOTSPOTS_FROM_MAP = new ConfigOption<>(
      BUILDER,
      "hotspots_from_map",
      "Generate hotspots from map or procedurally",
      true
    );
    KOPPEN_FROM_MAP = new ConfigOption<>(
      BUILDER,
      "koppen_from_map",
      "Generate climate parameters (temperature, rainfall, rainfall variance) from Köppen climate map or procedurally",
      true
    );

    BUILDER.pop();
    SPEC = BUILDER.build();

    allOptions = Arrays.asList(
      MAP_PROFILE,
      SPAWN_MODE,
      SPAWN_CENTER_LONGITUDE,
      SPAWN_CENTER_LATITUDE,
      SPAWN_CENTER_X,
      SPAWN_CENTER_Z,
      SPAWN_DISTANCE,
      CANYONS_NOT_VOLCANIC,
      FLAT_BEDROCK,
      FINITE_CONTINENTS,
      CONTINENTALNESS,
      GRASS_DENSITY,
      TEMPERATURE_CONSTANT,
      RAINFALL_CONSTANT,
      TEMPERATURE_SCALE,
      RAINFALL_SCALE,
      HORIZONTAL_SCALE,
      VERTICAL_SCALE,
      CONTINENT_FROM_MAP,
      ALTITUDE_FROM_MAP,
      HOTSPOTS_FROM_MAP,
      KOPPEN_FROM_MAP
    );
  }

  public static void setServerConfig(
    String mapProfile,
    SpawnMode spawnMode,
    double spawnCenterLongitude,
    double spawnCenterLatitude,
    int spawnCenterX,
    int spawnCenterZ,
    int spawnDistance,
    boolean canyonsNotVolcanic,
    boolean flatBedrock,
    boolean finiteContinents,
    double continentalness,
    double grassDensity,
    double temperatureConstant,
    double rainfallConstant,
    int temperatureScale,
    int rainfallScale,
    int horizontalScale,
    int verticalScale,
    boolean continentFromMap,
    boolean altitudeFromMap,
    boolean hotspotsFromMap,
    boolean koppenFromMap
  ) {
    MAP_PROFILE.setServerValue(mapProfile);
    SPAWN_MODE.setServerValue(spawnMode);
    SPAWN_CENTER_LONGITUDE.setServerValue(spawnCenterLongitude);
    SPAWN_CENTER_LATITUDE.setServerValue(spawnCenterLatitude);
    SPAWN_CENTER_X.setServerValue(spawnCenterX);
    SPAWN_CENTER_Z.setServerValue(spawnCenterZ);
    SPAWN_DISTANCE.setServerValue(spawnDistance);
    CANYONS_NOT_VOLCANIC.setServerValue(canyonsNotVolcanic);
    FLAT_BEDROCK.setServerValue(flatBedrock);
    FINITE_CONTINENTS.setServerValue(finiteContinents);
    CONTINENTALNESS.setServerValue(continentalness);
    GRASS_DENSITY.setServerValue(grassDensity);
    TEMPERATURE_CONSTANT.setServerValue(temperatureConstant);
    RAINFALL_CONSTANT.setServerValue(rainfallConstant);
    TEMPERATURE_SCALE.setServerValue(temperatureScale);
    RAINFALL_SCALE.setServerValue(rainfallScale);
    HORIZONTAL_SCALE.setServerValue(horizontalScale);
    VERTICAL_SCALE.setServerValue(verticalScale);
    CONTINENT_FROM_MAP.setServerValue(continentFromMap);
    ALTITUDE_FROM_MAP.setServerValue(altitudeFromMap);
    HOTSPOTS_FROM_MAP.setServerValue(hotspotsFromMap);
    KOPPEN_FROM_MAP.setServerValue(koppenFromMap);
  }

  public static void clearServerConfig() {
    allOptions.forEach(ConfigOption::clearServerValue);
  }

  public static void setModConfig(ModConfig config) {
    modConfig = config;
  }

  public static void saveConfig() {
    if (modConfig != null) {
      try {
        var loadedConfig = modConfig.getLoadedConfig();
        if (loadedConfig != null) {
          loadedConfig.save();
        }
      } catch (Exception e) {
        TFCRealWorld.LOGGER.warn("Failed to save config to file", e);
      }
    }
  }

  private static double getSpawnCenterLongitude() {
    return getProfile().spawnCenterLongitude();
  }

  private static double getSpawnCenterLatitude() {
    return getProfile().spawnCenterLatitude();
  }

  private static int getSpawnCenterX() {
    return getProfile().getSpawnCenterX();
  }

  private static int getSpawnCenterZ() {
    return getProfile().getSpawnCenterZ();
  }

  private static int getHorizontalScale() {
    return getProfile().horizontalScale();
  }

  private static int getVerticalScale() {
    return getProfile().verticalScale();
  }

  public static double getWestEdgeLongitude() {
    return getProfile().westEdgeLongitude();
  }

  public static double getEastEdgeLongitude() {
    return getProfile().eastEdgeLongitude();
  }

  public static double getSouthEdgeLatitude() {
    return getProfile().southEdgeLatitude();
  }

  public static double getNorthEdgeLatitude() {
    return getProfile().northEdgeLatitude();
  }

  public static MapProjection getMapProjection() {
    return getProfile().mapProjection();
  }

  private static MapProfile getProfile() {
    return ProfileManager.getProfile(
      SPEC == null ? DEFAULT_MAP_PROFILE : MAP_PROFILE.get()
    );
  }
}
