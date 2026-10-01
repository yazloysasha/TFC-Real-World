package net.yazloysasha.tfcrealworld.util.profile;

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import net.neoforged.fml.loading.FMLPaths;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.jetbrains.annotations.Nullable;

public class ProfileManager {

  public static final String SETTINGS_FILE = "settings.json";
  public static final String MAPS_DIR = "maps";

  private static final Map<String, MapProfile> PROFILE_CACHE = new HashMap<>();
  private static final Map<String, ProfileLocation> PROFILE_LOCATIONS =
    new HashMap<>();
  private static boolean initialized = false;

  public record ProfileLocation(
    String namespace,
    String profileName,
    boolean isZip,
    Path zipPath,
    Path directoryPath
  ) {}

  public static void initialize() {
    if (initialized) {
      return;
    }

    List<String> profileIds = new ArrayList<>();
    discoverProfilesFromExternal(profileIds);
    discoverProfilesFromJar(profileIds);

    if (profileIds.isEmpty()) {
      TFCRealWorld.LOGGER.warn("No profiles found, using default profile");
    }

    initialized = true;
  }

  public static List<String> discoverProfiles() {
    List<String> profileIds = new ArrayList<>(PROFILE_LOCATIONS.keySet());
    if (profileIds.isEmpty()) {
      return getDefaultProfileList();
    }
    Map<String, Integer> indexMap = new HashMap<>();
    for (String profileId : profileIds) {
      indexMap.put(profileId, getProfile(profileId).index());
    }
    profileIds.sort(Comparator.comparing(indexMap::get));
    return profileIds;
  }

  private static void discoverProfilesFromJar(List<String> profileIds) {
    String resourcePath = "/data/" + TFCRealWorld.MOD_ID + "/profiles/";

    try {
      URL resourceUrl = TFCRealWorld.class.getResource(resourcePath);
      if (resourceUrl == null) {
        return;
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
          discoverProfilesInPath(
            profilesResourcePath,
            profileIds,
            false,
            null,
            true
          );
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
    } catch (URISyntaxException | IOException e) {
      TFCRealWorld.LOGGER.error("Failed to discover profiles from JAR", e);
    }
  }

  private static void discoverProfilesFromExternal(List<String> profileIds) {
    Path configDir = FMLPaths.CONFIGDIR.get();
    Path profilesDir = configDir
      .resolve(TFCRealWorld.MOD_ID)
      .resolve("profiles");

    if (!Files.exists(profilesDir)) {
      try {
        Files.createDirectories(profilesDir);
      } catch (IOException e) {
        TFCRealWorld.LOGGER.error(
          "Failed to create profiles directory: {}",
          profilesDir,
          e
        );
        return;
      }
    }

    try (Stream<Path> paths = Files.list(profilesDir)) {
      paths.forEach(path -> {
        String fileName = path.getFileName().toString();
        if (Files.isDirectory(path)) {
          String namespace = fileName;
          discoverProfilesInNamespace(
            path,
            namespace,
            profileIds,
            false,
            null,
            false
          );
        } else if (fileName.toLowerCase().endsWith(".zip")) {
          discoverProfilesFromZip(path, profileIds, false);
        }
      });
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error(
        "Failed to list items in external profiles directory",
        e
      );
    }
  }

  private static void discoverProfilesInPath(
    Path profilesPath,
    List<String> profileIds,
    boolean isZip,
    Path zipPath,
    boolean skipIfExists
  ) {
    try (Stream<Path> namespacePaths = Files.list(profilesPath)) {
      namespacePaths
        .filter(Files::isDirectory)
        .forEach(namespacePath -> {
          String namespace = namespacePath.getFileName().toString();
          discoverProfilesInNamespace(
            namespacePath,
            namespace,
            profileIds,
            isZip,
            zipPath,
            skipIfExists
          );
        });
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error(
        "Failed to list namespaces in profiles directory",
        e
      );
    }
  }

  private static void discoverProfilesInNamespace(
    Path namespacePath,
    String namespace,
    List<String> profileIds,
    boolean isZip,
    Path zipPath,
    boolean skipIfExists
  ) {
    try (Stream<Path> paths = Files.list(namespacePath)) {
      paths.forEach(path -> {
        String fileName = path.getFileName().toString();
        if (Files.isDirectory(path)) {
          String profileName = fileName;
          Path settingsPath = path.resolve(SETTINGS_FILE);
          if (Files.exists(settingsPath)) {
            addProfile(
              namespace,
              profileName,
              profileIds,
              isZip,
              zipPath,
              isZip ? null : path,
              skipIfExists
            );
          }
        } else if (fileName.toLowerCase().endsWith(".zip")) {
          discoverProfilesFromZip(path, profileIds, skipIfExists);
        }
      });
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error(
        "Failed to list profiles in namespace: {}",
        namespace,
        e
      );
    }
  }

  private static void addProfile(
    String namespace,
    String profileName,
    List<String> profileIds,
    boolean isZip,
    Path zipPath,
    Path directoryPath,
    boolean skipIfExists
  ) {
    String profileId = buildProfileId(namespace, profileName);
    String upperProfileId = profileId.toUpperCase();

    if (skipIfExists && PROFILE_LOCATIONS.containsKey(upperProfileId)) {
      return;
    }

    if (!profileIds.contains(upperProfileId)) {
      profileIds.add(upperProfileId);
    }
    PROFILE_LOCATIONS.put(
      upperProfileId,
      new ProfileLocation(namespace, profileName, isZip, zipPath, directoryPath)
    );
  }

  private static void discoverProfilesFromZip(
    Path zipPath,
    List<String> profileIds,
    boolean skipIfExists
  ) {
    try (
      FileSystem zipFs = FileSystems.newFileSystem(
        zipPath,
        Collections.emptyMap()
      )
    ) {
      Path rootPath = zipFs.getPath("/");
      if (Files.exists(rootPath)) {
        discoverProfilesInPath(
          rootPath,
          profileIds,
          true,
          zipPath,
          skipIfExists
        );
      }
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error("Failed to read ZIP file: {}", zipPath, e);
    }
  }

  private static String buildProfileId(String namespace, String profileName) {
    return namespace + ":" + profileName;
  }

  public static String[] parseProfileId(String profileId) {
    int colonIndex = profileId.indexOf(':');
    return new String[] {
      profileId.substring(0, colonIndex),
      profileId.substring(colonIndex + 1),
    };
  }

  private static List<String> getDefaultProfileList() {
    return List.of(TFCRealWorldConfig.DEFAULT_MAP_PROFILE);
  }

  public static MapProfile getProfile(String profileId) {
    return PROFILE_CACHE.computeIfAbsent(
      profileId,
      MapProfile::loadFromResources
    );
  }

  public static InputStream getMapStream(String profileId, String mapName) {
    return getProfileFileStream(profileId, MAPS_DIR + "/" + mapName + ".png");
  }

  /**
   * A file of the profile by its path inside the profile folder, such as
   * {@code settings.json}, {@code tectonics.json} or {@code maps/continent.png}.
   * Falls back to the built-in profile of the same id.
   */
  @Nullable
  public static InputStream getProfileFileStream(
    String profileId,
    String path
  ) {
    final String[] parts = parseProfileId(profileId.toLowerCase());
    final String namespace = parts[0];
    final String profileName = parts[1];

    InputStream stream = null;
    final ProfileLocation location = PROFILE_LOCATIONS.get(
      profileId.toUpperCase()
    );
    if (location != null) {
      if (location.isZip()) {
        stream = getStreamFromZip(
          location.zipPath(),
          "/" + namespace + "/" + profileName + "/" + path
        );
      } else if (location.directoryPath() != null) {
        stream = getStreamFromDirectory(location.directoryPath(), path);
      }
    }
    if (stream != null) {
      return stream;
    }
    return TFCRealWorld.class.getResourceAsStream(
      "/data/" +
        TFCRealWorld.MOD_ID +
        "/profiles/" +
        namespace +
        "/" +
        profileName +
        "/" +
        path
    );
  }

  @Nullable
  private static InputStream getStreamFromZip(Path zipPath, String path) {
    return withZipFileSystem(zipPath, zipFs -> {
      try {
        final Path filePath = zipFs.getPath(path);
        if (Files.exists(filePath)) {
          return new ZipInputStreamWrapper(
            Files.newInputStream(filePath),
            zipFs
          );
        }
      } catch (IOException e) {
        TFCRealWorld.LOGGER.error(
          "Failed to read {} from ZIP: {}",
          path,
          zipPath,
          e
        );
      }
      return null;
    });
  }

  @Nullable
  private static InputStream getStreamFromDirectory(
    Path profilePath,
    String path
  ) {
    try {
      final Path filePath = profilePath.resolve(path);
      if (Files.exists(filePath)) {
        return Files.newInputStream(filePath);
      }
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error(
        "Failed to read {} from directory: {}",
        path,
        profilePath,
        e
      );
    }
    return null;
  }

  private static class ZipInputStreamWrapper extends InputStream {

    private final InputStream delegate;
    private final FileSystem fileSystem;

    public ZipInputStreamWrapper(InputStream delegate, FileSystem fileSystem) {
      this.delegate = delegate;
      this.fileSystem = fileSystem;
    }

    @Override
    public int read() throws IOException {
      return delegate.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
      return delegate.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      return delegate.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
      delegate.close();
      fileSystem.close();
    }
  }

  /**
   * Helper method to safely work with ZIP FileSystem.
   * Ensures proper resource cleanup even if exceptions occur.
   *
   * @param zipPath Path to the ZIP file
   * @param action Function to execute with the FileSystem
   * @return Result from action, or null if an error occurred
   */
  private static InputStream withZipFileSystem(
    Path zipPath,
    Function<FileSystem, InputStream> action
  ) {
    FileSystem zipFs = null;
    try {
      zipFs = FileSystems.newFileSystem(zipPath, Collections.emptyMap());
      InputStream result = action.apply(zipFs);
      if (result == null && zipFs != null) {
        zipFs.close();
        zipFs = null;
      }
      return result;
    } catch (IOException e) {
      if (zipFs != null) {
        try {
          zipFs.close();
        } catch (IOException ignored) {}
      }
      return null;
    }
  }
}
