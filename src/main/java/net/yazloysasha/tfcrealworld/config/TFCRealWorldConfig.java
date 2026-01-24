package net.yazloysasha.tfcrealworld.config;

import java.util.Arrays;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.types.MapProjection;
import net.yazloysasha.tfcrealworld.types.SpawnMode;
import net.yazloysasha.tfcrealworld.util.profile.MapProfile;
import net.yazloysasha.tfcrealworld.util.profile.ProfileManager;

public class TFCRealWorldConfig {

  public static final ForgeConfigSpec.Builder BUILDER =
    new ForgeConfigSpec.Builder();
  public static final ForgeConfigSpec SPEC;

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
  public static final ConfigOption<Boolean> FLAT_BEDROCK;
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
      "Spawn mode. Affects spawn location\n" +
      "  GEOGRAPHIC - spawn_center_longitude/latitude\n" +
      "  RANDOM - generate from seed\n" +
      "  CLASSIC - use spawn_center_x/z",
      SpawnMode.GEOGRAPHIC,
      SpawnMode.class
    );
    SPAWN_CENTER_LONGITUDE = new ConfigOption<>(
      BUILDER,
      "spawn_center_longitude",
      "Geographic longitude for spawn location (used when spawn_mode is GEOGRAPHIC)",
      getSpawnCenterLongitude(),
      -360.0,
      360.0
    );
    SPAWN_CENTER_LATITUDE = new ConfigOption<>(
      BUILDER,
      "spawn_center_latitude",
      "Geographic latitude for spawn location (used when spawn_mode is GEOGRAPHIC)",
      getSpawnCenterLatitude(),
      -90.0,
      90.0
    );

    BUILDER.pop();
    BUILDER.push("tfc_spawn_settings");

    SPAWN_CENTER_X = new ConfigOption<>(
      BUILDER,
      "spawn_center_x",
      "Spawn center X coordinate (used when spawn_mode is CLASSIC)",
      getSpawnCenterX(),
      -MAX_SCALE,
      MAX_SCALE
    );
    SPAWN_CENTER_Z = new ConfigOption<>(
      BUILDER,
      "spawn_center_z",
      "Spawn center Z coordinate (used when spawn_mode is CLASSIC)",
      getSpawnCenterZ(),
      -MAX_SCALE,
      MAX_SCALE
    );
    SPAWN_DISTANCE = new ConfigOption<>(
      BUILDER,
      "spawn_distance",
      "Spawn distance in blocks",
      MIN_SCALE,
      MIN_SCALE,
      MAX_SCALE
    );

    BUILDER.pop();
    BUILDER.push("world_generation");

    FLAT_BEDROCK = new ConfigOption<>(
      BUILDER,
      "flat_bedrock",
      "Whether bedrock is flat",
      false
    );
    CONTINENTALNESS = new ConfigOption<>(
      BUILDER,
      "continentalness",
      "Continentalness value",
      0.5,
      0.0,
      1.0
    );
    GRASS_DENSITY = new ConfigOption<>(
      BUILDER,
      "grass_density",
      "Grass density",
      0.5,
      0.0,
      1.0
    );
    TEMPERATURE_CONSTANT = new ConfigOption<>(
      BUILDER,
      "temperature_constant",
      "Temperature constant",
      0.0,
      -1.0,
      1.0
    );
    RAINFALL_CONSTANT = new ConfigOption<>(
      BUILDER,
      "rainfall_constant",
      "Rainfall constant",
      0.0,
      -1.0,
      1.0
    );
    TEMPERATURE_SCALE = new ConfigOption<>(
      BUILDER,
      "temperature_scale",
      "Temperature scale in blocks",
      DEFAULT_SCALE,
      MIN_SCALE,
      MAX_SCALE
    );
    RAINFALL_SCALE = new ConfigOption<>(
      BUILDER,
      "rainfall_scale",
      "Rainfall scale in blocks",
      DEFAULT_SCALE,
      MIN_SCALE,
      MAX_SCALE
    );

    BUILDER.pop();
    BUILDER.push("generation_modes");

    HORIZONTAL_SCALE = new ConfigOption<>(
      BUILDER,
      "horizontal_scale",
      "Horizontal map radius in blocks.",
      getHorizontalScale(),
      MIN_SCALE,
      MAX_SCALE
    );
    VERTICAL_SCALE = new ConfigOption<>(
      BUILDER,
      "vertical_scale",
      "Vertical map radius in blocks.",
      getVerticalScale(),
      MIN_SCALE,
      MAX_SCALE
    );
    CONTINENT_FROM_MAP = new ConfigOption<>(
      BUILDER,
      "continent_from_map",
      "Whether to generate continents from map (true) or procedurally (false)",
      true
    );
    ALTITUDE_FROM_MAP = new ConfigOption<>(
      BUILDER,
      "altitude_from_map",
      "Whether to generate base land height and ocean depth from altitude map (true) or procedurally (false). " +
      "Uses grayscale altitude.png where brightness 128 = sea level (0m), brightness 255 = highest elevation. " +
      "For land: values below sea level (brightness < 128) are treated as 0. " +
      "Land elevations (brightness >= 128) are mapped to baseLandHeight range (0-24). " +
      "For ocean: underwater areas (brightness < 128) are mapped to baseOceanDepth range (0-15), " +
      "where lower brightness (deeper) = higher depth value.",
      true
    );
    HOTSPOTS_FROM_MAP = new ConfigOption<>(
      BUILDER,
      "hotspots_from_map",
      "Whether to generate hotspots from map (true) or procedurally (false). Uses hotspots.png with grayscale values: 0 (NoActivity), 64 (Ancient), 127 (Extinct), 192 (Dormant), 255 (Active).",
      true
    );
    KOPPEN_FROM_MAP = new ConfigOption<>(
      BUILDER,
      "koppen_from_map",
      "Whether to generate climate parameters (temperature, rainfall, rainfall variance) from Köppen climate map (true) or procedurally (false). " +
      "When enabled, reads koppen.png map and generates procedural parameter values that are valid for each Köppen climate classification.",
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
      FLAT_BEDROCK,
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
    boolean flatBedrock,
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
    FLAT_BEDROCK.setServerValue(flatBedrock);
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
        SPEC.save();
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
