package net.yazloysasha.tfcrealworld.config;

import java.util.Arrays;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public class TFCRealWorldConfig {

  public enum SpawnMode {
    DEFAULT,
    GEOGRAPHIC,
    RANDOM,
  }

  public enum MapProjection {
    EQUAL_EARTH,
  }

  public static final ModConfigSpec.Builder BUILDER =
    new ModConfigSpec.Builder();
  public static final ModConfigSpec SPEC;

  // Spawn settings
  public static final ConfigOption<SpawnMode> SPAWN_MODE;
  public static final ConfigOption<Double> SPAWN_CENTER_LONGITUDE;
  public static final ConfigOption<Double> SPAWN_CENTER_LATITUDE;

  // World generation settings
  public static final ConfigOption<Integer> SPAWN_CENTER_X;
  public static final ConfigOption<Integer> SPAWN_CENTER_Z;
  public static final ConfigOption<Integer> SPAWN_DISTANCE;
  public static final ConfigOption<Boolean> FLAT_BEDROCK;
  public static final ConfigOption<Boolean> FINITE_CONTINENTS;
  public static final ConfigOption<Double> CONTINENTALNESS;
  public static final ConfigOption<Double> GRASS_DENSITY;
  public static final ConfigOption<Integer> TEMPERATURE_SCALE;
  public static final ConfigOption<Integer> RAINFALL_SCALE;

  // Generation mode settings
  public static final ConfigOption<Integer> HORIZONTAL_TILE_SIZE;
  public static final ConfigOption<Integer> VERTICAL_TILE_SIZE;
  public static final ConfigOption<Boolean> CONTINENT_FROM_MAP;
  public static final ConfigOption<Boolean> ALTITUDE_FROM_MAP;
  public static final ConfigOption<Boolean> HOTSPOTS_FROM_MAP;
  public static final ConfigOption<Boolean> KOPPEN_FROM_MAP;

  // Biome modifications settings
  public static final ConfigOption<Boolean> CANYONS_NOT_VOLCANIC;

  // Map settings
  public static final ConfigOption<Double> WEST_EDGE_LONGITUDE;
  public static final ConfigOption<Double> EAST_EDGE_LONGITUDE;
  public static final ConfigOption<Double> SOUTH_EDGE_LATITUDE;
  public static final ConfigOption<Double> NORTH_EDGE_LATITUDE;
  public static final ConfigOption<MapProjection> MAP_PROJECTION;

  private static final List<ConfigOption<?>> allOptions;

  static {
    BUILDER.comment("TFC: Real World Configuration");
    BUILDER.push("spawn_settings");

    SPAWN_MODE = new ConfigOption<>(
      BUILDER,
      "spawn_mode",
      "Spawn mode. Affects spawn location\n" +
      "  DEFAULT - use spawn_center_x/z\n" +
      "  GEOGRAPHIC - spawn_center_longitude/latitude\n" +
      "  RANDOM - generate from seed",
      SpawnMode.DEFAULT,
      SpawnMode.class
    );
    SPAWN_CENTER_LONGITUDE = new ConfigOption<>(
      BUILDER,
      "spawn_center_longitude",
      "Geographic longitude for spawn location (used when spawn_mode is GEOGRAPHIC)",
      0.0, // TODO: Выставить правильно сконвертированные координаты с текущих X/Z
      -180.0,
      180.0
    );
    SPAWN_CENTER_LATITUDE = new ConfigOption<>(
      BUILDER,
      "spawn_center_latitude",
      "Geographic latitude for spawn location (used when spawn_mode is GEOGRAPHIC)",
      0.0, // TODO: Выставить правильно сконвертированные координаты с текущих X/Z
      -90.0,
      90.0
    );

    BUILDER.pop();
    BUILDER.push("world_generation");

    SPAWN_CENTER_X = new ConfigOption<>(
      BUILDER,
      "spawn_center_x",
      "Spawn center X coordinate (used when spawn_mode is DEFAULT)",
      -9000,
      -100000,
      100000
    );
    SPAWN_CENTER_Z = new ConfigOption<>(
      BUILDER,
      "spawn_center_z",
      "Spawn center Z coordinate (used when spawn_mode is DEFAULT)",
      -3000,
      -100000,
      100000
    );
    SPAWN_DISTANCE = new ConfigOption<>(
      BUILDER,
      "spawn_distance",
      "Spawn distance in blocks",
      100,
      0,
      10000
    );
    FLAT_BEDROCK = new ConfigOption<>(
      BUILDER,
      "flat_bedrock",
      "Whether bedrock is flat",
      false
    );
    FINITE_CONTINENTS = new ConfigOption<>(
      BUILDER,
      "finite_continents",
      "Whether continents are finite",
      false
    );
    CONTINENTALNESS = new ConfigOption<>(
      BUILDER,
      "continentalness",
      "Continentalness value (0.0 to 1.0)",
      0.5,
      0.0,
      1.0
    );
    GRASS_DENSITY = new ConfigOption<>(
      BUILDER,
      "grass_density",
      "Grass density (0.0 to 1.0)",
      0.5,
      0.0,
      1.0
    );
    TEMPERATURE_SCALE = new ConfigOption<>(
      BUILDER,
      "temperature_scale",
      "Temperature scale in blocks",
      20000,
      1000,
      100000
    );
    RAINFALL_SCALE = new ConfigOption<>(
      BUILDER,
      "rainfall_scale",
      "Rainfall scale in blocks",
      20000,
      1000,
      100000
    );

    BUILDER.pop();
    BUILDER.push("generation_modes");

    HORIZONTAL_TILE_SIZE = new ConfigOption<>(
      BUILDER,
      "horizontal_tile_size",
      "Horizontal tile size (diameter) in blocks. Affects horizontal map stretching when generating from map.",
      40000,
      1000,
      200000
    );
    VERTICAL_TILE_SIZE = new ConfigOption<>(
      BUILDER,
      "vertical_tile_size",
      "Vertical tile size (diameter) in blocks. Affects vertical map stretching when generating from map.",
      40000,
      1000,
      200000
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
    BUILDER.push("biome_modifications");

    CANYONS_NOT_VOLCANIC = new ConfigOption<>(
      BUILDER,
      "canyons_not_volcanic",
      "Whether canyons and doline_canyons biomes should have volcanic features removed.",
      true
    );

    BUILDER.pop();
    BUILDER.push("map_settings");
    BUILDER.comment(
      " !!! I don't recommend changing it if you're not sure !!!"
    );

    WEST_EDGE_LONGITUDE = new ConfigOption<>(
      BUILDER,
      "west_edge_longitude",
      "Western boundary of the map extent in degrees longitude",
      -20.0,
      -180.0,
      180.0
    );
    EAST_EDGE_LONGITUDE = new ConfigOption<>(
      BUILDER,
      "east_edge_longitude",
      "Eastern boundary of the map extent in degrees longitude",
      160.0,
      -180.0,
      180.0
    );
    SOUTH_EDGE_LATITUDE = new ConfigOption<>(
      BUILDER,
      "south_edge_latitude",
      "Southern boundary of the map extent in degrees latitude",
      -90.0,
      -90.0,
      90.0
    );
    NORTH_EDGE_LATITUDE = new ConfigOption<>(
      BUILDER,
      "north_edge_latitude",
      "Northern boundary of the map extent in degrees latitude",
      90.0,
      -90.0,
      90.0
    );
    MAP_PROJECTION = new ConfigOption<>(
      BUILDER,
      "map_projection",
      "Map projection type for geographic coordinate conversion\n" +
      "  EQUAL_EARTH - equal-area, preserving the scale of areas",
      MapProjection.EQUAL_EARTH,
      MapProjection.class
    );

    BUILDER.pop();
    SPEC = BUILDER.build();

    allOptions = Arrays.asList(
      SPAWN_MODE,
      SPAWN_CENTER_LONGITUDE,
      SPAWN_CENTER_LATITUDE,
      SPAWN_CENTER_X,
      SPAWN_CENTER_Z,
      SPAWN_DISTANCE,
      FLAT_BEDROCK,
      FINITE_CONTINENTS,
      CONTINENTALNESS,
      GRASS_DENSITY,
      TEMPERATURE_SCALE,
      RAINFALL_SCALE,
      HORIZONTAL_TILE_SIZE,
      VERTICAL_TILE_SIZE,
      CONTINENT_FROM_MAP,
      ALTITUDE_FROM_MAP,
      HOTSPOTS_FROM_MAP,
      KOPPEN_FROM_MAP,
      CANYONS_NOT_VOLCANIC,
      WEST_EDGE_LONGITUDE,
      EAST_EDGE_LONGITUDE,
      SOUTH_EDGE_LATITUDE,
      NORTH_EDGE_LATITUDE,
      MAP_PROJECTION
    );
  }

  public static void setServerConfig(
    SpawnMode spawnMode,
    Double spawnCenterLongtitude,
    Double spawnCenterLatitude,
    int spawnCenterX,
    int spawnCenterZ,
    int spawnDistance,
    boolean flatBedrock,
    boolean finiteContinents,
    double continentalness,
    double grassDensity,
    int temperatureScale,
    int rainfallScale,
    int horizontalTileSize,
    int verticalTileSize,
    boolean continentFromMap,
    boolean altitudeFromMap,
    boolean hotspotsFromMap,
    boolean koppenFromMap,
    boolean canyonsNotVolcanic,
    Double westEdgeLongtitude,
    Double eastEdgeLongtitude,
    Double southEdgeLatitude,
    Double northEdgeLatitude,
    MapProjection mapProjection
  ) {
    SPAWN_MODE.setServerValue(spawnMode);
    SPAWN_CENTER_LONGITUDE.setServerValue(spawnCenterLongtitude);
    SPAWN_CENTER_LATITUDE.setServerValue(spawnCenterLatitude);
    SPAWN_CENTER_X.setServerValue(spawnCenterX);
    SPAWN_CENTER_Z.setServerValue(spawnCenterZ);
    SPAWN_DISTANCE.setServerValue(spawnDistance);
    FLAT_BEDROCK.setServerValue(flatBedrock);
    FINITE_CONTINENTS.setServerValue(finiteContinents);
    CONTINENTALNESS.setServerValue(continentalness);
    GRASS_DENSITY.setServerValue(grassDensity);
    TEMPERATURE_SCALE.setServerValue(temperatureScale);
    RAINFALL_SCALE.setServerValue(rainfallScale);
    HORIZONTAL_TILE_SIZE.setServerValue(horizontalTileSize);
    VERTICAL_TILE_SIZE.setServerValue(verticalTileSize);
    CONTINENT_FROM_MAP.setServerValue(continentFromMap);
    ALTITUDE_FROM_MAP.setServerValue(altitudeFromMap);
    HOTSPOTS_FROM_MAP.setServerValue(hotspotsFromMap);
    KOPPEN_FROM_MAP.setServerValue(koppenFromMap);
    CANYONS_NOT_VOLCANIC.setServerValue(canyonsNotVolcanic);
    WEST_EDGE_LONGITUDE.setServerValue(westEdgeLongtitude);
    EAST_EDGE_LONGITUDE.setServerValue(eastEdgeLongtitude);
    SOUTH_EDGE_LATITUDE.setServerValue(southEdgeLatitude);
    NORTH_EDGE_LATITUDE.setServerValue(northEdgeLatitude);
    MAP_PROJECTION.setServerValue(mapProjection);
  }

  public static void clearServerConfig() {
    allOptions.forEach(ConfigOption::clearServerValue);
  }

  public static int getHemisphereScale() {
    if (KOPPEN_FROM_MAP.get()) {
      return (int) (VERTICAL_TILE_SIZE.get() / 2);
    } else {
      return TEMPERATURE_SCALE.get();
    }
  }
}
