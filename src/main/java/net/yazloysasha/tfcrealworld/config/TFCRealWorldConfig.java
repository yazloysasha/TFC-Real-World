package net.yazloysasha.tfcrealworld.config;

import java.util.Arrays;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public class TFCRealWorldConfig {

  public static final ModConfigSpec.Builder BUILDER =
    new ModConfigSpec.Builder();
  public static final ModConfigSpec SPEC;

  // World generation settings
  public static final ConfigOption<Double> CONTINENTALNESS;
  public static final ConfigOption<Boolean> FINITE_CONTINENTS;
  public static final ConfigOption<Boolean> FLAT_BEDROCK;
  public static final ConfigOption<Double> GRASS_DENSITY;
  public static final ConfigOption<Integer> SPAWN_CENTER_X;
  public static final ConfigOption<Integer> SPAWN_CENTER_Z;
  public static final ConfigOption<Integer> SPAWN_DISTANCE;
  public static final ConfigOption<Integer> TEMPERATURE_SCALE;
  public static final ConfigOption<Integer> RAINFALL_SCALE;

  // Generation mode settings
  public static final ConfigOption<Integer> VERTICAL_WORLD_SCALE;
  public static final ConfigOption<Integer> HORIZONTAL_WORLD_SCALE;
  public static final ConfigOption<Boolean> CONTINENT_FROM_MAP;
  public static final ConfigOption<Boolean> ALTITUDE_FROM_MAP;
  public static final ConfigOption<Boolean> HOTSPOTS_FROM_MAP;
  public static final ConfigOption<Boolean> KOPPEN_FROM_MAP;
  public static final ConfigOption<Integer> POLE_OFFSET;
  public static final ConfigOption<Boolean> POLE_LOOPING;
  public static final ConfigOption<Boolean> CANYONS_NOT_VOLCANIC;

  private static final List<ConfigOption<?>> allOptions;

  static {
    BUILDER.comment("TFC: Real World Configuration").push("world_generation");

    CONTINENTALNESS = new ConfigOption<>(
      BUILDER,
      "continentalness",
      "Continentalness value (0.0 to 1.0)",
      0.5,
      0.0,
      1.0
    );
    FINITE_CONTINENTS = new ConfigOption<>(
      BUILDER,
      "finite_continents",
      "Whether continents are finite",
      false
    );
    FLAT_BEDROCK = new ConfigOption<>(
      BUILDER,
      "flat_bedrock",
      "Whether bedrock is flat",
      false
    );
    GRASS_DENSITY = new ConfigOption<>(
      BUILDER,
      "grass_density",
      "Grass density (0.0 to 1.0)",
      0.5,
      0.0,
      1.0
    );
    SPAWN_CENTER_X = new ConfigOption<>(
      BUILDER,
      "spawn_center_x",
      "Spawn center X coordinate",
      -9000,
      -100000,
      100000
    );
    SPAWN_CENTER_Z = new ConfigOption<>(
      BUILDER,
      "spawn_center_z",
      "Spawn center Z coordinate",
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
    TEMPERATURE_SCALE = new ConfigOption<>(
      BUILDER,
      "temperature_scale",
      "Temperature scale in blocks",
      40000,
      1000,
      100000
    );
    RAINFALL_SCALE = new ConfigOption<>(
      BUILDER,
      "rainfall_scale",
      "Rainfall scale in blocks",
      40000,
      1000,
      100000
    );

    BUILDER.pop();
    BUILDER.push("generation_modes");

    VERTICAL_WORLD_SCALE = new ConfigOption<>(
      BUILDER,
      "vertical_world_scale",
      "Vertical world scale (diameter) in blocks. Affects distance between poles and globe_trotter achievement.",
      40000,
      1000,
      200000
    );
    HORIZONTAL_WORLD_SCALE = new ConfigOption<>(
      BUILDER,
      "horizontal_world_scale",
      "Horizontal world scale (diameter) in blocks. Affects map stretching when generating from map.",
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
    POLE_OFFSET = new ConfigOption<>(
      BUILDER,
      "pole_offset",
      "Pole offset in blocks",
      10000,
      -100000,
      100000
    );
    POLE_LOOPING = new ConfigOption<>(
      BUILDER,
      "pole_looping",
      "Whether poles should loop (cyclical)",
      false
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
    SPEC = BUILDER.build();

    allOptions = Arrays.asList(
      CONTINENTALNESS,
      FINITE_CONTINENTS,
      FLAT_BEDROCK,
      GRASS_DENSITY,
      SPAWN_CENTER_X,
      SPAWN_CENTER_Z,
      SPAWN_DISTANCE,
      TEMPERATURE_SCALE,
      RAINFALL_SCALE,
      VERTICAL_WORLD_SCALE,
      HORIZONTAL_WORLD_SCALE,
      CONTINENT_FROM_MAP,
      ALTITUDE_FROM_MAP,
      HOTSPOTS_FROM_MAP,
      KOPPEN_FROM_MAP,
      POLE_OFFSET,
      POLE_LOOPING,
      CANYONS_NOT_VOLCANIC
    );
  }

  public static void setServerConfig(
    double continentalness,
    boolean finiteContinents,
    boolean flatBedrock,
    double grassDensity,
    int spawnCenterX,
    int spawnCenterZ,
    int spawnDistance,
    int temperatureScale,
    int rainfallScale,
    int verticalWorldScale,
    int horizontalWorldScale,
    boolean continentFromMap,
    boolean altitudeFromMap,
    boolean hotspotsFromMap,
    boolean koppenFromMap,
    int poleOffset,
    boolean poleLooping,
    boolean canyonsNotVolcanic
  ) {
    CONTINENTALNESS.setServerValue(continentalness);
    FINITE_CONTINENTS.setServerValue(finiteContinents);
    FLAT_BEDROCK.setServerValue(flatBedrock);
    GRASS_DENSITY.setServerValue(grassDensity);
    SPAWN_CENTER_X.setServerValue(spawnCenterX);
    SPAWN_CENTER_Z.setServerValue(spawnCenterZ);
    SPAWN_DISTANCE.setServerValue(spawnDistance);
    TEMPERATURE_SCALE.setServerValue(temperatureScale);
    RAINFALL_SCALE.setServerValue(rainfallScale);
    VERTICAL_WORLD_SCALE.setServerValue(verticalWorldScale);
    HORIZONTAL_WORLD_SCALE.setServerValue(horizontalWorldScale);
    CONTINENT_FROM_MAP.setServerValue(continentFromMap);
    ALTITUDE_FROM_MAP.setServerValue(altitudeFromMap);
    HOTSPOTS_FROM_MAP.setServerValue(hotspotsFromMap);
    KOPPEN_FROM_MAP.setServerValue(koppenFromMap);
    POLE_OFFSET.setServerValue(poleOffset);
    POLE_LOOPING.setServerValue(poleLooping);
    CANYONS_NOT_VOLCANIC.setServerValue(canyonsNotVolcanic);
  }

  public static void clearServerConfig() {
    allOptions.forEach(ConfigOption::clearServerValue);
  }
}
