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
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.neoforged.fml.loading.FMLPaths;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

public class ProfileManager {

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
          Path settingsPath = path.resolve("settings.json");
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

  public static ProfileLocation getProfileLocation(String profileId) {
    return PROFILE_LOCATIONS.get(profileId.toUpperCase());
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

  /**
   * Match {@code maps/xN} tier folders. Available tiers are discovered from the
   * profile filesystem / ZIP / classpath resource tree (not a hardcoded probe list).
   * Midpoints between consecutive discovered tiers decide selection
   * (x1 < 1.5 <= x2; later x4 uses midpoint 3, …). Climate maps typically exist
   * only under x2; continent/hotspots under x1, x2, and x4 in the built-in profiles.
   */
  private static final Pattern MAP_TIER_DIR = Pattern.compile(
    "^x([0-9]+(?:\\.[0-9]+)?)$",
    Pattern.CASE_INSENSITIVE
  );

  public static InputStream getMapStream(String profileId, String mapName) {
    String[] parts = parseProfileId(profileId.toLowerCase());
    String namespace = parts[0];
    String profileName = parts[1];
    String tierFolder = resolveMapTierFolder(profileId, mapName);

    ProfileLocation location = PROFILE_LOCATIONS.get(profileId.toUpperCase());
    if (location != null) {
      if (location.isZip()) {
        return getMapStreamFromZip(
          location.zipPath(),
          namespace,
          profileName,
          mapName,
          tierFolder
        );
      } else if (location.directoryPath() != null) {
        return getMapStreamFromDirectory(
          location.directoryPath(),
          mapName,
          tierFolder
        );
      }
    }

    if (tierFolder != null) {
      InputStream scaled =
        TFCRealWorld.class.getResourceAsStream(
            resourceMapPath(namespace, profileName, tierFolder, mapName)
          );
      if (scaled != null) {
        return scaled;
      }
    }
    return TFCRealWorld.class.getResourceAsStream(
        resourceMapPath(namespace, profileName, null, mapName)
      );
  }

  private static String resourceMapPath(
    String namespace,
    String profileName,
    String tierFolder,
    String mapName
  ) {
    StringBuilder path = new StringBuilder();
    path
      .append("/data/")
      .append(TFCRealWorld.MOD_ID)
      .append("/profiles/")
      .append(namespace)
      .append("/")
      .append(profileName)
      .append("/maps/");
    if (tierFolder != null && !tierFolder.isEmpty()) {
      path.append(tierFolder).append("/");
    }
    path.append(mapName).append(".png");
    return path.toString();
  }

  /**
   * Pick {@code maps/xN} for {@code mapName}: highest available tier whose
   * midpoint-range covers the current world scale factor vs profile defaults.
   * Maps present only under x2 (climate) always resolve to x2 even when the
   * scale factor would prefer x1 for continent/hotspots.
   */
  public static String resolveMapTierFolder(String profileId, String mapName) {
    List<Double> available = listAvailableMapTiers(profileId, mapName);
    if (available.isEmpty()) {
      return null;
    }
    double factor = currentScaleFactor(profileId);
    double chosen = selectMapTier(available, factor);
    return formatMapTierFolder(chosen);
  }

  public static double currentScaleFactor(String profileId) {
    MapProfile profile = getProfile(profileId);
    double defaultH = Math.max(1, profile.horizontalScale());
    double defaultV = Math.max(1, profile.verticalScale());
    double factorH = TFCRealWorldConfig.HORIZONTAL_SCALE.get() / defaultH;
    double factorV = TFCRealWorldConfig.VERTICAL_SCALE.get() / defaultV;
    return Math.max(factorH, factorV);
  }

  public static double selectMapTier(List<Double> sortedTiers, double factor) {
    if (sortedTiers == null || sortedTiers.isEmpty()) {
      return 2.0;
    }
    double chosen = sortedTiers.get(0);
    for (int i = 1; i < sortedTiers.size(); i++) {
      double mid = (sortedTiers.get(i - 1) + sortedTiers.get(i)) / 2.0;
      if (factor >= mid) {
        chosen = sortedTiers.get(i);
      } else {
        break;
      }
    }
    return chosen;
  }

  public static String formatMapTierFolder(double tier) {
    if (Math.rint(tier) == tier) {
      return "x" + (int) Math.rint(tier);
    }
    return "x" + trimTierString(tier);
  }

  private static String trimTierString(double tier) {
    String s = String.format(Locale.ROOT, "%.4f", tier);
    while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
      s = s.substring(0, s.length() - 1);
    }
    return s;
  }

  private static List<Double> listAvailableMapTiers(
    String profileId,
    String mapName
  ) {
    String[] parts = parseProfileId(profileId.toLowerCase());
    String namespace = parts[0];
    String profileName = parts[1];
    ProfileLocation location = PROFILE_LOCATIONS.get(profileId.toUpperCase());

    List<Double> found = new ArrayList<>();
    if (location != null && location.isZip()) {
      found.addAll(
        listTiersInZip(location.zipPath(), namespace, profileName, mapName)
      );
    } else if (location != null && location.directoryPath() != null) {
      found.addAll(listTiersInDirectory(location.directoryPath(), mapName));
    }

    if (found.isEmpty()) {
      // Built-in JAR / classpath profiles: list maps/xN dirs from the resource tree.
      found.addAll(listTiersInClasspath(namespace, profileName, mapName));
    }

    found.sort(Double::compareTo);
    return found;
  }

  private static List<Double> listTiersInDirectory(
    Path profilePath,
    String mapName
  ) {
    return collectTiersFromMapsRoot(profilePath.resolve("maps"), mapName);
  }

  /**
   * Discover {@code maps/xN/<mapName>.png} tiers by listing the profile maps
   * folder on the classpath (dev {@code file:} tree or packed {@code jar:}).
   */
  private static List<Double> listTiersInClasspath(
    String namespace,
    String profileName,
    String mapName
  ) {
    List<Double> found = new ArrayList<>();
    String resourcePath =
      "/data/" +
      TFCRealWorld.MOD_ID +
      "/profiles/" +
      namespace +
      "/" +
      profileName +
      "/maps";

    try {
      URL resourceUrl = TFCRealWorld.class.getResource(resourcePath);
      if (resourceUrl == null) {
        resourceUrl = TFCRealWorld.class.getResource(resourcePath + "/");
      }
      if (resourceUrl == null) {
        return found;
      }

      URI resourceUri = resourceUrl.toURI();
      FileSystem fileSystem = null;
      boolean closeFileSystem = false;
      try {
        Path mapsRoot;
        if ("jar".equals(resourceUri.getScheme())) {
          String uriString = resourceUri.toString();
          int bang = uriString.indexOf("!/");
          URI jarUri = bang >= 0
            ? URI.create(uriString.substring(0, bang))
            : resourceUri;
          try {
            fileSystem = FileSystems.getFileSystem(jarUri);
          } catch (java.nio.file.FileSystemNotFoundException e) {
            fileSystem = FileSystems.newFileSystem(
              jarUri,
              Collections.emptyMap()
            );
            closeFileSystem = true;
          }
          mapsRoot = fileSystem.getPath(resourcePath);
        } else {
          mapsRoot = Paths.get(resourceUri);
        }
        found.addAll(collectTiersFromMapsRoot(mapsRoot, mapName));
      } finally {
        if (closeFileSystem && fileSystem != null) {
          try {
            fileSystem.close();
          } catch (IOException ignored) {}
        }
      }
    } catch (URISyntaxException | IOException e) {
      TFCRealWorld.LOGGER.error(
        "Failed to list map tiers from classpath for {}/{} map {}",
        namespace,
        profileName,
        mapName,
        e
      );
    }
    return found;
  }

  private static List<Double> collectTiersFromMapsRoot(
    Path mapsRoot,
    String mapName
  ) {
    List<Double> found = new ArrayList<>();
    if (mapsRoot == null || !Files.isDirectory(mapsRoot)) {
      return found;
    }
    try (Stream<Path> stream = Files.list(mapsRoot)) {
      stream
        .filter(Files::isDirectory)
        .forEach(dir -> {
          Matcher matcher = MAP_TIER_DIR.matcher(dir.getFileName().toString());
          if (!matcher.matches()) {
            return;
          }
          if (Files.exists(dir.resolve(mapName + ".png"))) {
            found.add(Double.parseDouble(matcher.group(1)));
          }
        });
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error(
        "Failed to list map tiers in maps root: {}",
        mapsRoot,
        e
      );
    }
    return found;
  }

  private static List<Double> listTiersInZip(
    Path zipPath,
    String namespace,
    String profileName,
    String mapName
  ) {
    List<Double> found = new ArrayList<>();
    withZipFileSystem(zipPath, zipFs -> {
      Path mapsRoot = zipFs.getPath(
        "/" + namespace + "/" + profileName + "/maps"
      );
      found.addAll(collectTiersFromMapsRoot(mapsRoot, mapName));
      return null;
    });
    return found;
  }

  private static InputStream getMapStreamFromZip(
    Path zipPath,
    String namespace,
    String profileName,
    String mapName,
    String tierFolder
  ) {
    return withZipFileSystem(zipPath, zipFs -> {
      try {
        Path mapsRoot = zipFs.getPath(
          "/" + namespace + "/" + profileName + "/maps"
        );
        if (tierFolder != null) {
          Path scaled = mapsRoot.resolve(tierFolder).resolve(mapName + ".png");
          if (Files.exists(scaled)) {
            return new ZipInputStreamWrapper(
              Files.newInputStream(scaled),
              zipFs
            );
          }
        }
        Path legacy = mapsRoot.resolve(mapName + ".png");
        if (Files.exists(legacy)) {
          return new ZipInputStreamWrapper(Files.newInputStream(legacy), zipFs);
        }
      } catch (IOException e) {
        TFCRealWorld.LOGGER.error(
          "Failed to read map from ZIP: {}",
          zipPath,
          e
        );
      }
      return null;
    });
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

  private static InputStream getMapStreamFromDirectory(
    Path profilePath,
    String mapName,
    String tierFolder
  ) {
    try {
      Path mapsRoot = profilePath.resolve("maps");
      if (tierFolder != null) {
        Path scaled = mapsRoot.resolve(tierFolder).resolve(mapName + ".png");
        if (Files.exists(scaled)) {
          return Files.newInputStream(scaled);
        }
      }
      Path legacy = mapsRoot.resolve(mapName + ".png");
      if (Files.exists(legacy)) {
        return Files.newInputStream(legacy);
      }
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error(
        "Failed to read map from directory: {}",
        profilePath,
        e
      );
    }
    return null;
  }

  static InputStream getSettingsStreamFromZip(
    Path zipPath,
    String namespace,
    String profileName
  ) {
    return withZipFileSystem(zipPath, zipFs -> {
      try {
        Path settingsPath = zipFs.getPath(
          "/" + namespace + "/" + profileName + "/settings.json"
        );
        if (Files.exists(settingsPath)) {
          return new ZipInputStreamWrapper(
            Files.newInputStream(settingsPath),
            zipFs
          );
        }
      } catch (IOException e) {
        TFCRealWorld.LOGGER.error(
          "Failed to read settings from ZIP: {}",
          zipPath,
          e
        );
      }
      return null;
    });
  }

  static InputStream getSettingsStreamFromDirectory(Path profilePath) {
    try {
      Path settingsPath = profilePath.resolve("settings.json");
      if (Files.exists(settingsPath)) {
        return Files.newInputStream(settingsPath);
      }
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error(
        "Failed to read settings from directory: {}",
        profilePath,
        e
      );
    }
    return null;
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
