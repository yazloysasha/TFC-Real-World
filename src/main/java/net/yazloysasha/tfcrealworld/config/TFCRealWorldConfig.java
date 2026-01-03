package net.yazloysasha.tfcrealworld.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class TFCRealWorldConfig {

  public static final ModConfigSpec.Builder BUILDER =
    new ModConfigSpec.Builder();
  public static final ModConfigSpec SPEC;

  // World generation settings (from overworld.json -> tfc_settings, excluding rock_layer_settings)
  public static final ModConfigSpec.DoubleValue CONTINENTALNESS;
  public static final ModConfigSpec.BooleanValue FINITE_CONTINENTS;
  public static final ModConfigSpec.BooleanValue FLAT_BEDROCK;
  public static final ModConfigSpec.DoubleValue GRASS_DENSITY;
  public static final ModConfigSpec.IntValue SPAWN_CENTER_X;
  public static final ModConfigSpec.IntValue SPAWN_CENTER_Z;
  public static final ModConfigSpec.IntValue SPAWN_DISTANCE;
  public static final ModConfigSpec.IntValue TEMPERATURE_SCALE;
  public static final ModConfigSpec.IntValue RAINFALL_SCALE;

  // Generation mode settings
  public static final ModConfigSpec.IntValue VERTICAL_WORLD_SCALE;
  public static final ModConfigSpec.IntValue HORIZONTAL_WORLD_SCALE;
  public static final ModConfigSpec.BooleanValue CONTINENT_FROM_MAP;
  public static final ModConfigSpec.BooleanValue ALTITUDE_FROM_MAP;
  public static final ModConfigSpec.BooleanValue HOTSPOTS_FROM_MAP;
  public static final ModConfigSpec.BooleanValue KOPPEN_FROM_MAP;
  public static final ModConfigSpec.IntValue POLE_OFFSET;
  public static final ModConfigSpec.BooleanValue POLE_LOOPING;
  public static final ModConfigSpec.BooleanValue CANYONS_NOT_VOLCANIC;

  static {
    BUILDER.comment("TFC: Real World Configuration").push("world_generation");

    CONTINENTALNESS = BUILDER.comment(
      "Continentalness value (0.0 to 1.0)"
    ).defineInRange("continentalness", 0.5, 0.0, 1.0);

    BUILDER.comment("");

    FINITE_CONTINENTS = BUILDER.comment("Whether continents are finite").define(
      "finite_continents",
      false
    );

    BUILDER.comment("");

    FLAT_BEDROCK = BUILDER.comment("Whether bedrock is flat").define(
      "flat_bedrock",
      false
    );

    BUILDER.comment("");

    GRASS_DENSITY = BUILDER.comment("Grass density (0.0 to 1.0)").defineInRange(
      "grass_density",
      0.5,
      0.0,
      1.0
    );

    BUILDER.comment("");

    SPAWN_CENTER_X = BUILDER.comment("Spawn center X coordinate").defineInRange(
      "spawn_center_x",
      -9000,
      -100000,
      100000
    );

    BUILDER.comment("");

    SPAWN_CENTER_Z = BUILDER.comment("Spawn center Z coordinate").defineInRange(
      "spawn_center_z",
      -3000,
      -100000,
      100000
    );

    BUILDER.comment("");

    SPAWN_DISTANCE = BUILDER.comment("Spawn distance in blocks").defineInRange(
      "spawn_distance",
      100,
      0,
      10000
    );

    BUILDER.comment("");

    TEMPERATURE_SCALE = BUILDER.comment(
      "Temperature scale in blocks"
    ).defineInRange("temperature_scale", 40000, 1000, 100000);

    BUILDER.comment("");

    RAINFALL_SCALE = BUILDER.comment("Rainfall scale in blocks").defineInRange(
      "rainfall_scale",
      40000,
      1000,
      100000
    );

    BUILDER.pop();
    BUILDER.push("generation_modes");

    VERTICAL_WORLD_SCALE = BUILDER.comment(
      "Vertical world scale (diameter) in blocks. Affects distance between poles and globe_trotter achievement."
    ).defineInRange("vertical_world_scale", 40000, 1000, 200000);

    BUILDER.comment("");

    HORIZONTAL_WORLD_SCALE = BUILDER.comment(
      "Horizontal world scale (diameter) in blocks. Affects map stretching when generating from map."
    ).defineInRange("horizontal_world_scale", 40000, 1000, 200000);

    BUILDER.comment("");

    CONTINENT_FROM_MAP = BUILDER.comment(
      "Whether to generate continents from map (true) or procedurally (false)"
    ).define("continent_from_map", true);

    BUILDER.comment("");

    ALTITUDE_FROM_MAP = BUILDER.comment(
      "Whether to generate base land height and ocean depth from altitude map (true) or procedurally (false). " +
      "Uses grayscale altitude.png where brightness 128 = sea level (0m), brightness 255 = highest elevation. " +
      "For land: values below sea level (brightness < 128) are treated as 0. " +
      "Land elevations (brightness >= 128) are mapped to baseLandHeight range (0-24). " +
      "For ocean: underwater areas (brightness < 128) are mapped to baseOceanDepth range (0-15), " +
      "where lower brightness (deeper) = higher depth value."
    ).define("altitude_from_map", true);

    BUILDER.comment("");

    HOTSPOTS_FROM_MAP = BUILDER.comment(
      "Whether to generate hotspots from map (true) or procedurally (false). Uses hotspots.png with grayscale values: 0 (NoActivity), 64 (Ancient), 127 (Extinct), 192 (Dormant), 255 (Active)."
    ).define("hotspots_from_map", true);

    BUILDER.comment("");

    KOPPEN_FROM_MAP = BUILDER.comment(
      "Whether to generate climate parameters (temperature, rainfall, rainfall variance) from Köppen climate map (true) or procedurally (false). " +
      "When enabled, reads koppen.png map and generates procedural parameter values that are valid for each Köppen climate classification."
    ).define("koppen_from_map", true);

    BUILDER.comment("");

    POLE_OFFSET = BUILDER.comment("Pole offset in blocks").defineInRange(
      "pole_offset",
      10000,
      -100000,
      100000
    );

    BUILDER.comment("");

    POLE_LOOPING = BUILDER.comment(
      "Whether poles should loop (cyclical)"
    ).define("pole_looping", false);

    BUILDER.pop();
    BUILDER.push("biome_modifications");

    BUILDER.comment(
      "Whether canyons and doline_canyons biomes should have volcanic features removed."
    );
    CANYONS_NOT_VOLCANIC = BUILDER.define("canyons_not_volcanic", true);

    BUILDER.pop();
    SPEC = BUILDER.build();
  }

  public static double getContinentalness() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getContinentalness() != null
    ) {
      return ServerConfigValues.getContinentalness();
    }
    return CONTINENTALNESS.get();
  }

  public static boolean getFiniteContinents() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getFiniteContinents() != null
    ) {
      return ServerConfigValues.getFiniteContinents();
    }
    return FINITE_CONTINENTS.get();
  }

  public static boolean getFlatBedrock() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getFlatBedrock() != null
    ) {
      return ServerConfigValues.getFlatBedrock();
    }
    return FLAT_BEDROCK.get();
  }

  public static double getGrassDensity() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getGrassDensity() != null
    ) {
      return ServerConfigValues.getGrassDensity();
    }
    return GRASS_DENSITY.get();
  }

  public static int getSpawnCenterX() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getSpawnCenterX() != null
    ) {
      return ServerConfigValues.getSpawnCenterX();
    }
    return SPAWN_CENTER_X.get();
  }

  public static int getSpawnCenterZ() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getSpawnCenterZ() != null
    ) {
      return ServerConfigValues.getSpawnCenterZ();
    }
    return SPAWN_CENTER_Z.get();
  }

  public static int getSpawnDistance() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getSpawnDistance() != null
    ) {
      return ServerConfigValues.getSpawnDistance();
    }
    return SPAWN_DISTANCE.get();
  }

  public static int getTemperatureScale() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getTemperatureScale() != null
    ) {
      return ServerConfigValues.getTemperatureScale();
    }
    return TEMPERATURE_SCALE.get();
  }

  public static int getRainfallScale() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getRainfallScale() != null
    ) {
      return ServerConfigValues.getRainfallScale();
    }
    return RAINFALL_SCALE.get();
  }

  public static int getVerticalWorldScale() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getVerticalWorldScale() != null
    ) {
      return ServerConfigValues.getVerticalWorldScale();
    }
    return VERTICAL_WORLD_SCALE.get();
  }

  public static int getHorizontalWorldScale() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getHorizontalWorldScale() != null
    ) {
      return ServerConfigValues.getHorizontalWorldScale();
    }
    return HORIZONTAL_WORLD_SCALE.get();
  }

  public static boolean getContinentFromMap() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getContinentFromMap() != null
    ) {
      return ServerConfigValues.getContinentFromMap();
    }
    return CONTINENT_FROM_MAP.get();
  }

  public static boolean getAltitudeFromMap() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getAltitudeFromMap() != null
    ) {
      return ServerConfigValues.getAltitudeFromMap();
    }
    return ALTITUDE_FROM_MAP.get();
  }

  public static boolean getHotspotsFromMap() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getHotspotsFromMap() != null
    ) {
      return ServerConfigValues.getHotspotsFromMap();
    }
    return HOTSPOTS_FROM_MAP.get();
  }

  public static boolean getKoppenFromMap() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getKoppenFromMap() != null
    ) {
      return ServerConfigValues.getKoppenFromMap();
    }
    return KOPPEN_FROM_MAP.get();
  }

  public static int getPoleOffset() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getPoleOffset() != null
    ) {
      return ServerConfigValues.getPoleOffset();
    }
    return POLE_OFFSET.get();
  }

  public static boolean getPoleLooping() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getPoleLooping() != null
    ) {
      return ServerConfigValues.getPoleLooping();
    }
    return POLE_LOOPING.get();
  }

  public static boolean getCanyonsNotVolcanic() {
    if (
      ServerConfigValues.isServerConfigActive() &&
      ServerConfigValues.getCanyonsNotVolcanic() != null
    ) {
      return ServerConfigValues.getCanyonsNotVolcanic();
    }
    return CANYONS_NOT_VOLCANIC.get();
  }
}
