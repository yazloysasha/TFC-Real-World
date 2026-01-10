package net.yazloysasha.tfcrealworld;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.network.ConfigSyncPacket;
import net.yazloysasha.tfcrealworld.util.helpers.MapPathHelper;
import net.yazloysasha.tfcrealworld.util.pack.DynamicPackFinder;
import net.yazloysasha.tfcrealworld.world.noise.koppen.KoppenParameterCache;
import net.yazloysasha.tfcrealworld.world.noise.png.BasePNGNoise;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalOceanDistanceCache;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalWestCoastDistanceCache;
import org.slf4j.Logger;

@Mod(TFCRealWorld.MOD_ID)
public final class TFCRealWorld {

  public static final String MOD_ID = "tfc_real_world";
  public static final String MOD_NAME = "TFC: Real World";
  public static final Logger LOGGER = LogUtils.getLogger();

  public TFCRealWorld(ModContainer container, IEventBus modEventBus) {
    container.registerConfig(
      ModConfig.Type.COMMON,
      TFCRealWorldConfig.SPEC,
      "tfc_real_world/common.toml"
    );

    modEventBus.addListener(ModConfigEvent.Loading.class, event -> {
      if (event.getConfig().getModId().equals(MOD_ID)) {
        TFCRealWorldConfig.setModConfig(event.getConfig());
      }
    });

    modEventBus.addListener(
      AddPackFindersEvent.class,
      DynamicPackFinder::registerPack
    );

    modEventBus.addListener(
      RegisterPayloadHandlersEvent.class,
      this::registerNetwork
    );

    NeoForge.EVENT_BUS.addListener(
      PlayerEvent.PlayerLoggedInEvent.class,
      this::onPlayerLoggedIn
    );

    NeoForge.EVENT_BUS.addListener(
      PlayerEvent.PlayerLoggedOutEvent.class,
      this::onPlayerLoggedOut
    );

    NeoForge.EVENT_BUS.addListener(
      ClientPlayerNetworkEvent.LoggingIn.class,
      this::onClientLoggingIn
    );

    NeoForge.EVENT_BUS.addListener(
      ClientPlayerNetworkEvent.LoggingOut.class,
      this::onClientLoggingOut
    );

    NeoForge.EVENT_BUS.addListener(
      LevelEvent.Unload.class,
      this::onLevelUnload
    );

    setupMapsDirectory();
  }

  private void registerNetwork(RegisterPayloadHandlersEvent event) {
    final PayloadRegistrar registrar = event.registrar(TFCRealWorld.MOD_ID);
    registrar.playToClient(
      ConfigSyncPacket.TYPE,
      ConfigSyncPacket.STREAM_CODEC,
      ConfigSyncPacket::handle
    );
  }

  private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      ConfigSyncPacket packet = new ConfigSyncPacket(
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
        TFCRealWorldConfig.KOPPEN_FROM_MAP.get(),
        TFCRealWorldConfig.WEST_EDGE_LONGITUDE.get(),
        TFCRealWorldConfig.EAST_EDGE_LONGITUDE.get(),
        TFCRealWorldConfig.SOUTH_EDGE_LATITUDE.get(),
        TFCRealWorldConfig.NORTH_EDGE_LATITUDE.get(),
        TFCRealWorldConfig.MAP_PROJECTION.get()
      );
      serverPlayer.connection.send(packet);
    }
  }

  private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
    if (!event.getEntity().level().isClientSide) {
      return;
    }

    // For safety, clear client-side view of server config when the local player logs out
    TFCRealWorldConfig.clearServerConfig();
  }

  private void onLevelUnload(LevelEvent.Unload event) {
    if (event.getLevel().isClientSide()) {
      TFCRealWorldConfig.clearServerConfig();
    }
  }

  private void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
    try {
      Minecraft mc = Minecraft.getInstance();
      boolean isSingleplayer =
        mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null;

      if (!isSingleplayer) {
        clearCaches();
      }
    } catch (Exception e) {
      clearCaches();
    }
  }

  private void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    // Always clear client-side view of server config when the network connection closes
    TFCRealWorldConfig.clearServerConfig();
  }

  private void clearCaches() {
    GlobalOceanDistanceCache.clear();
    GlobalWestCoastDistanceCache.clear();
    KoppenParameterCache.clear();
    BasePNGNoise.clearImageCache();
  }

  private void setupMapsDirectory() {
    try {
      Path mapsDir = MapPathHelper.getMapsDirectory();

      if (!Files.exists(mapsDir)) {
        Files.createDirectories(mapsDir);
      }

      List<String> mapNames = discoverMapsFromResources();

      for (String mapName : mapNames) {
        Path mapPath = MapPathHelper.getMapPath(mapName);
        if (!Files.exists(mapPath)) {
          copyMapFromResources(mapName, mapPath);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Failed to setup maps directory", e);
    }
  }

  private List<String> discoverMapsFromResources() {
    List<String> mapNames = new ArrayList<>();
    String resourcePath = "/assets/tfc_real_world/maps/";

    try {
      URL resourceUrl = TFCRealWorld.class.getResource(resourcePath);
      if (resourceUrl == null) {
        return getDefaultMapList();
      }

      URI resourceUri = resourceUrl.toURI();
      Path mapsResourcePath;
      FileSystem fileSystem = null;

      try {
        if (resourceUri.getScheme().equals("jar")) {
          fileSystem = FileSystems.newFileSystem(
            resourceUri,
            Collections.emptyMap()
          );
          mapsResourcePath = fileSystem.getPath(resourcePath);
        } else {
          mapsResourcePath = Paths.get(resourceUri);
        }

        if (Files.exists(mapsResourcePath)) {
          try (Stream<Path> paths = Files.list(mapsResourcePath)) {
            paths
              .filter(Files::isRegularFile)
              .filter(path -> path.getFileName().toString().endsWith(".png"))
              .forEach(path -> {
                String fileName = path.getFileName().toString();
                String mapName = fileName.substring(0, fileName.length() - 4);
                mapNames.add(mapName);
              });
          }
        }

        if (fileSystem != null) {
          fileSystem.close();
        }
      } catch (IOException e) {
        if (fileSystem != null) {
          try {
            fileSystem.close();
          } catch (IOException ignored) {}
        }
        throw e;
      }

      if (mapNames.isEmpty()) {
        return getDefaultMapList();
      }

      return mapNames;
    } catch (URISyntaxException | IOException e) {
      return getDefaultMapList();
    }
  }

  private List<String> getDefaultMapList() {
    List<String> defaultMaps = new ArrayList<>();
    defaultMaps.add("continents");
    defaultMaps.add("altitude");
    defaultMaps.add("hotspots");
    defaultMaps.add("koppen");
    return defaultMaps;
  }

  private void copyMapFromResources(String mapName, Path targetPath) {
    String resourcePath = "/assets/tfc_real_world/maps/" + mapName + ".png";
    try (
      InputStream resourceStream =
        TFCRealWorld.class.getResourceAsStream(resourcePath)
    ) {
      if (resourceStream == null) {
        return;
      }

      Files.copy(
        resourceStream,
        targetPath,
        StandardCopyOption.REPLACE_EXISTING
      );
    } catch (IOException e) {
      LOGGER.error(
        "Failed to copy default map {} to: {}",
        mapName,
        targetPath,
        e
      );
    }
  }
}
