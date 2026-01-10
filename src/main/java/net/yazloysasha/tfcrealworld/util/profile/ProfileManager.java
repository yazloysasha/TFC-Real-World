package net.yazloysasha.tfcrealworld.util.profile;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.slf4j.Logger;

public class ProfileManager {

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Map<String, MapProfile> PROFILE_CACHE = new HashMap<>();
  private static List<String> availableProfiles = null;

  public static List<String> discoverProfiles() {
    if (availableProfiles != null) {
      return availableProfiles;
    }

    List<String> profileIds = new ArrayList<>();
    String resourcePath = "/data/" + TFCRealWorld.MOD_ID + "/profiles/";

    try {
      URL resourceUrl = TFCRealWorld.class.getResource(resourcePath);
      if (resourceUrl == null) {
        LOGGER.warn("Profiles directory not found at: {}", resourcePath);
        availableProfiles = getDefaultProfileList();
        return availableProfiles;
      }

      URI resourceUri = resourceUrl.toURI();
      Path profilesResourcePath;
      FileSystem fileSystem = null;

      try {
        if (resourceUri.getScheme().equals("jar")) {
          fileSystem = FileSystems.newFileSystem(
            resourceUri,
            Collections.emptyMap()
          );
          profilesResourcePath = fileSystem.getPath(resourcePath);
        } else {
          profilesResourcePath = Paths.get(resourceUri);
        }

        if (Files.exists(profilesResourcePath)) {
          try (Stream<Path> paths = Files.list(profilesResourcePath)) {
            paths
              .filter(Files::isDirectory)
              .forEach(path -> {
                String profileId = path.getFileName().toString();
                Path settingsPath = path.resolve("settings.json");
                if (Files.exists(settingsPath)) {
                  profileIds.add(profileId.toUpperCase());
                }
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

      if (profileIds.isEmpty()) {
        availableProfiles = getDefaultProfileList();
      } else {
        availableProfiles = profileIds;
      }
    } catch (URISyntaxException | IOException e) {
      LOGGER.error("Failed to discover profiles from resources", e);
      availableProfiles = getDefaultProfileList();
    }

    return availableProfiles;
  }

  private static List<String> getDefaultProfileList() {
    List<String> defaultProfiles = new ArrayList<>();
    defaultProfiles.add(TFCRealWorldConfig.DEFAULT_MAP_PROFILE);
    return defaultProfiles;
  }

  public static MapProfile getProfile(String profileId) {
    return PROFILE_CACHE.computeIfAbsent(profileId, id -> {
      MapProfile profile = MapProfile.loadFromResources(id);
      LOGGER.info("Loaded profile: {}", id);
      return profile;
    });
  }

  public static void clearCache() {
    PROFILE_CACHE.clear();
    availableProfiles = null;
  }

  public static InputStream getMapStream(String profileId, String mapName) {
    String resourcePath =
      "/data/" +
      TFCRealWorld.MOD_ID +
      "/profiles/" +
      profileId.toLowerCase() +
      "/maps/" +
      mapName +
      ".png";
    return TFCRealWorld.class.getResourceAsStream(resourcePath);
  }
}
