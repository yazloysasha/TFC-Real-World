package net.yazloysasha.tfcrealworld;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.DynamicPackFinder;
import net.yazloysasha.tfcrealworld.util.MapPathHelper;
import org.slf4j.Logger;

@Mod(TFCRealWorld.MOD_ID)
public final class TFCRealWorld {

  public static final String MOD_ID = "tfc_real_world";
  public static final String MOD_NAME = "TFC: Real World";
  public static final Logger LOGGER = LogUtils.getLogger();

  public TFCRealWorld(ModContainer container, IEventBus modEventBus) {
    LOGGER.info(
      "Initializing {} v{}",
      MOD_NAME,
      container.getModInfo().getVersion()
    );

    container.registerConfig(
      ModConfig.Type.COMMON,
      TFCRealWorldConfig.SPEC,
      "tfc_real_world/common.toml"
    );

    modEventBus.addListener(
      AddPackFindersEvent.class,
      DynamicPackFinder::registerPack
    );

    setupMapsDirectory();
  }

  private void setupMapsDirectory() {
    try {
      Path mapsDir = MapPathHelper.getMapsDirectory();

      if (!Files.exists(mapsDir)) {
        Files.createDirectories(mapsDir);
        LOGGER.info("Created maps directory at: {}", mapsDir);
      }

      List<String> mapNames = discoverMapsFromResources();
      LOGGER.info(
        "Discovered {} maps in resources: {}",
        mapNames.size(),
        mapNames
      );

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
      java.net.URL resourceUrl = TFCRealWorld.class.getResource(resourcePath);
      if (resourceUrl == null) {
        LOGGER.warn("Maps resource directory not found, using default list");
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
        LOGGER.warn("No maps discovered in resources, using default list");
        return getDefaultMapList();
      }

      return mapNames;
    } catch (URISyntaxException | IOException e) {
      LOGGER.warn(
        "Failed to discover maps from resources, falling back to default list",
        e
      );
      return getDefaultMapList();
    }
  }

  private List<String> getDefaultMapList() {
    List<String> defaultMaps = new ArrayList<>();
    defaultMaps.add("continent");
    defaultMaps.add("altitude");
    defaultMaps.add("hotspots");
    defaultMaps.add("temperature");
    defaultMaps.add("rainfall");
    return defaultMaps;
  }

  private void copyMapFromResources(String mapName, Path targetPath) {
    String resourcePath = "/assets/tfc_real_world/maps/" + mapName + ".png";
    try (
      InputStream resourceStream =
        TFCRealWorld.class.getResourceAsStream(resourcePath)
    ) {
      if (resourceStream == null) {
        LOGGER.warn(
          "Default map {} not found in resources at: {}",
          mapName,
          resourcePath
        );
        return;
      }

      Files.copy(
        resourceStream,
        targetPath,
        StandardCopyOption.REPLACE_EXISTING
      );
      LOGGER.info("Copied default map {} to: {}", mapName, targetPath);
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
