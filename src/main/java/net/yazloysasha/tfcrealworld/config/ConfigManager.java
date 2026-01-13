package net.yazloysasha.tfcrealworld.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.network.ConfigSyncPacket;
import net.yazloysasha.tfcrealworld.types.SpawnMode;
import net.yazloysasha.tfcrealworld.util.DataPackGenerator;

public class ConfigManager {

  private static final String COMMON_CONFIG_NAME =
    TFCRealWorld.MOD_ID + "/common.toml";
  private static final String SERVER_CONFIG_NAME =
    TFCRealWorld.MOD_ID + "/server.toml";

  private static Path getServerConfigPath(MinecraftServer server) {
    Path worldPath = server.getWorldPath(
      net.minecraft.world.level.storage.LevelResource.ROOT
    );
    return worldPath.resolve("serverconfig").resolve(SERVER_CONFIG_NAME);
  }

  @SubscribeEvent
  public static void onServerAboutToStart(ServerAboutToStartEvent event) {
    MinecraftServer server = event.getServer();

    try {
      Path serverConfigFile = getServerConfigPath(server);
      Path worldConfigDir = serverConfigFile.getParent();
      Path commonConfigFile = server
        .getServerDirectory()
        .resolve("config")
        .resolve(COMMON_CONFIG_NAME);

      if (!Files.exists(serverConfigFile) && Files.exists(commonConfigFile)) {
        Files.createDirectories(worldConfigDir);
        Files.copy(
          commonConfigFile,
          serverConfigFile,
          StandardCopyOption.REPLACE_EXISTING
        );
        TFCRealWorld.LOGGER.info("Server config created from common.toml");
      }

      loadServerConfig(serverConfigFile);
      DataPackGenerator.generateDataPack(server);
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error("Error working with server config", e);
    }
  }

  private static void loadServerConfig(Path configPath) throws IOException {
    if (!Files.exists(configPath)) {
      TFCRealWorld.LOGGER.warn("Server config file not found: {}", configPath);
      return;
    }

    try {
      CommentedConfig config = TomlFormat.instance().createConfig();
      try (Reader reader = Files.newBufferedReader(configPath)) {
        TomlFormat.instance()
          .createParser()
          .parse(reader, config, ParsingMode.REPLACE);
      }

      TFCRealWorldConfig.setServerConfig(
        getString(
          config,
          "map_settings.map_profile",
          TFCRealWorldConfig.MAP_PROFILE.get()
        ),
        getEnum(
          config,
          "spawn_settings.spawn_mode",
          TFCRealWorldConfig.SPAWN_MODE.get(),
          SpawnMode.class
        ),
        getDouble(
          config,
          "spawn_settings.spawn_center_longitude",
          TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.get()
        ),
        getDouble(
          config,
          "spawn_settings.spawn_center_latitude",
          TFCRealWorldConfig.SPAWN_CENTER_LATITUDE.get()
        ),
        getInt(
          config,
          "tfc_spawn_settings.spawn_center_x",
          TFCRealWorldConfig.SPAWN_CENTER_X.get()
        ),
        getInt(
          config,
          "tfc_spawn_settings.spawn_center_z",
          TFCRealWorldConfig.SPAWN_CENTER_Z.get()
        ),
        getInt(
          config,
          "tfc_spawn_settings.spawn_distance",
          TFCRealWorldConfig.SPAWN_DISTANCE.get()
        ),
        getBoolean(
          config,
          "biome_modifications.canyons_not_volcanic",
          TFCRealWorldConfig.CANYONS_NOT_VOLCANIC.get()
        ),
        getBoolean(
          config,
          "world_generation.flat_bedrock",
          TFCRealWorldConfig.FLAT_BEDROCK.get()
        ),
        getBoolean(
          config,
          "world_generation.finite_continents",
          TFCRealWorldConfig.FINITE_CONTINENTS.get()
        ),
        getDouble(
          config,
          "world_generation.continentalness",
          TFCRealWorldConfig.CONTINENTALNESS.get()
        ),
        getDouble(
          config,
          "world_generation.grass_density",
          TFCRealWorldConfig.GRASS_DENSITY.get()
        ),
        getDouble(
          config,
          "world_generation.temperature_constant",
          TFCRealWorldConfig.TEMPERATURE_CONSTANT.get()
        ),
        getDouble(
          config,
          "world_generation.rainfall_constant",
          TFCRealWorldConfig.RAINFALL_CONSTANT.get()
        ),
        getInt(
          config,
          "world_generation.temperature_scale",
          TFCRealWorldConfig.TEMPERATURE_SCALE.get()
        ),
        getInt(
          config,
          "world_generation.rainfall_scale",
          TFCRealWorldConfig.RAINFALL_SCALE.get()
        ),
        getInt(
          config,
          "generation_modes.horizontal_tile_size",
          TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get()
        ),
        getInt(
          config,
          "generation_modes.vertical_tile_size",
          TFCRealWorldConfig.VERTICAL_TILE_SIZE.get()
        ),
        getBoolean(
          config,
          "generation_modes.continent_from_map",
          TFCRealWorldConfig.CONTINENT_FROM_MAP.get()
        ),
        getBoolean(
          config,
          "generation_modes.altitude_from_map",
          TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()
        ),
        getBoolean(
          config,
          "generation_modes.hotspots_from_map",
          TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()
        ),
        getBoolean(
          config,
          "generation_modes.koppen_from_map",
          TFCRealWorldConfig.KOPPEN_FROM_MAP.get()
        )
      );

      TFCRealWorld.LOGGER.info("Server config loaded: {}", configPath);
    } catch (ParsingException e) {
      TFCRealWorld.LOGGER.error("Error parsing TOML: {}", e.getMessage());
    }
  }

  private static String getString(
    CommentedConfig config,
    String path,
    String defaultValue
  ) {
    Object value = config.get(path);
    return value != null ? String.valueOf(value) : defaultValue;
  }

  private static int getInt(
    CommentedConfig config,
    String path,
    int defaultValue
  ) {
    Object value = config.get(path);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return defaultValue;
  }

  private static double getDouble(
    CommentedConfig config,
    String path,
    double defaultValue
  ) {
    Object value = config.get(path);
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    return defaultValue;
  }

  private static boolean getBoolean(
    CommentedConfig config,
    String path,
    boolean defaultValue
  ) {
    Object value = config.get(path);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return defaultValue;
  }

  private static <E extends Enum<E>> E getEnum(
    CommentedConfig config,
    String path,
    E defaultValue,
    Class<E> enumClass
  ) {
    String value = config.getOrElse(path, defaultValue.name());
    try {
      return Enum.valueOf(enumClass, value);
    } catch (IllegalArgumentException e) {
      TFCRealWorld.LOGGER.warn(
        "Invalid enum value '{}' for path '{}', using default: {}",
        value,
        path,
        defaultValue
      );
      return defaultValue;
    }
  }

  public static void reloadServerConfig(MinecraftServer server) {
    Path serverConfigFile = getServerConfigPath(server);

    try {
      loadServerConfig(serverConfigFile);

      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
        sendConfigToClient(player);
      }
    } catch (Exception e) {
      TFCRealWorld.LOGGER.error("Error reloading server config", e);
    }
  }

  public static void sendConfigToClient(ServerPlayer player) {
    ConfigSyncPacket packet = new ConfigSyncPacket(
      TFCRealWorldConfig.MAP_PROFILE.get(),
      TFCRealWorldConfig.SPAWN_MODE.get(),
      TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.get(),
      TFCRealWorldConfig.SPAWN_CENTER_LATITUDE.get(),
      TFCRealWorldConfig.SPAWN_CENTER_X.get(),
      TFCRealWorldConfig.SPAWN_CENTER_Z.get(),
      TFCRealWorldConfig.SPAWN_DISTANCE.get(),
      TFCRealWorldConfig.CANYONS_NOT_VOLCANIC.get(),
      TFCRealWorldConfig.FLAT_BEDROCK.get(),
      TFCRealWorldConfig.FINITE_CONTINENTS.get(),
      TFCRealWorldConfig.CONTINENTALNESS.get(),
      TFCRealWorldConfig.GRASS_DENSITY.get(),
      TFCRealWorldConfig.TEMPERATURE_CONSTANT.get(),
      TFCRealWorldConfig.RAINFALL_CONSTANT.get(),
      TFCRealWorldConfig.TEMPERATURE_SCALE.get(),
      TFCRealWorldConfig.RAINFALL_SCALE.get(),
      TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get(),
      TFCRealWorldConfig.VERTICAL_TILE_SIZE.get(),
      TFCRealWorldConfig.CONTINENT_FROM_MAP.get(),
      TFCRealWorldConfig.ALTITUDE_FROM_MAP.get(),
      TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get(),
      TFCRealWorldConfig.KOPPEN_FROM_MAP.get()
    );
    player.connection.send(packet);
  }
}
