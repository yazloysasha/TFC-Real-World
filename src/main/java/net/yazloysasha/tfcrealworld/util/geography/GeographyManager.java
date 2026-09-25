package net.yazloysasha.tfcrealworld.util.geography;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import net.neoforged.fml.loading.FMLPaths;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import org.jetbrains.annotations.Nullable;

public final class GeographyManager {

  private static final Gson GSON = new GsonBuilder().create();
  private static final Map<String, GeographyNode> NODES = new HashMap<>();
  private static boolean initialized = false;

  private GeographyManager() {}

  public static synchronized void initialize() {
    if (initialized) {
      return;
    }
    NODES.clear();
    discoverFromExternal();
    discoverFromJar();
    initialized = true;
    TFCRealWorld.LOGGER.info("Loaded {} geography nodes", NODES.size());
  }

  public static @Nullable GeographyNode get(String ref) {
    if (!initialized) {
      initialize();
    }
    return NODES.get(normalizeRef(ref));
  }

  public static java.util.Collection<GeographyNode> allNodes() {
    if (!initialized) {
      initialize();
    }
    return java.util.Collections.unmodifiableCollection(NODES.values());
  }

  public static java.util.List<GeographyNode> allWaypoints() {
    java.util.List<GeographyNode> out = new java.util.ArrayList<>();
    for (GeographyNode node : allNodes()) {
      if (node.kind() == GeographyKind.WAYPOINT) {
        out.add(node);
      }
    }
    return out;
  }

  public static String langKey(GeographyNode node, String suffix) {
    return (
      TFCRealWorld.MOD_ID +
      ".geography." +
      node.namespace() +
      "." +
      node.kind().path() +
      "." +
      node.slug() +
      "." +
      suffix
    );
  }

  public static @Nullable String resolveLang(
    String translationKey,
    String languageCode
  ) {
    if (!translationKey.startsWith(TFCRealWorld.MOD_ID + ".geography.")) {
      return null;
    }
    String rest = translationKey.substring(
      (TFCRealWorld.MOD_ID + ".geography.").length()
    );
    int lastDot = rest.lastIndexOf('.');
    if (lastDot <= 0) {
      return null;
    }
    String suffix = rest.substring(lastDot + 1);
    String body = rest.substring(0, lastDot);
    for (GeographyKind kind : GeographyKind.values()) {
      String marker = "." + kind.path() + ".";
      int idx = body.indexOf(marker);
      if (idx <= 0) {
        continue;
      }
      String namespace = body.substring(0, idx);
      String slug = body.substring(idx + marker.length());
      GeographyNode node = kind == GeographyKind.WAYPOINT
        ? get(namespace + ":" + slug)
        : get(namespace + ":" + kind.path() + "/" + slug);
      if (node == null) {
        return null;
      }
      if ("title".equals(suffix)) {
        return node.getTitle(languageCode);
      }
      if ("description".equals(suffix)) {
        return node.getDescription(languageCode);
      }
      if ("subtitle".equals(suffix)) {
        String sub = node.getSubtitle(languageCode);
        return sub.isBlank() ? null : sub;
      }
      return null;
    }
    return null;
  }

  private static String normalizeRef(String ref) {
    return ref.toLowerCase(Locale.ROOT);
  }

  private static void discoverFromJar() {
    String resourcePath = "/data/" + TFCRealWorld.MOD_ID + "/geography/";
    try {
      URL resourceUrl = TFCRealWorld.class.getResource(resourcePath);
      if (resourceUrl == null) {
        return;
      }
      URI resourceUri = resourceUrl.toURI();
      FileSystem fileSystem = null;
      try {
        Path geographyRoot;
        if ("jar".equals(resourceUri.getScheme())) {
          fileSystem = FileSystems.newFileSystem(
            resourceUri,
            Collections.emptyMap()
          );
          geographyRoot = fileSystem.getPath(resourcePath);
        } else {
          geographyRoot = Paths.get(resourceUri);
        }
        if (Files.exists(geographyRoot)) {
          discoverNamespaces(geographyRoot, true);
        }
      } finally {
        if (fileSystem != null) {
          fileSystem.close();
        }
      }
    } catch (URISyntaxException | IOException e) {
      TFCRealWorld.LOGGER.error("Failed to discover geography from JAR", e);
    }
  }

  private static void discoverFromExternal() {
    Path geographyDir = FMLPaths.CONFIGDIR.get()
      .resolve(TFCRealWorld.MOD_ID)
      .resolve("geography");
    if (!Files.exists(geographyDir)) {
      try {
        Files.createDirectories(geographyDir);
      } catch (IOException e) {
        TFCRealWorld.LOGGER.error(
          "Failed to create geography directory: {}",
          geographyDir,
          e
        );
        return;
      }
    }
    try (Stream<Path> paths = Files.list(geographyDir)) {
      paths.forEach(path -> {
        String fileName = path.getFileName().toString();
        if (fileName.startsWith(".")) {
          return;
        }
        if (Files.isDirectory(path)) {
          loadNamespace(path, fileName, false);
        } else if (fileName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
          discoverFromZip(path, false);
        }
      });
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error(
        "Failed to list items in external geography directory",
        e
      );
    }
  }

  private static void discoverFromZip(Path zipPath, boolean skipIfExists) {
    try (
      FileSystem zipFs = FileSystems.newFileSystem(
        zipPath,
        Collections.emptyMap()
      )
    ) {
      Path rootPath = zipFs.getPath("/");
      if (Files.exists(rootPath)) {
        discoverNamespaces(rootPath, skipIfExists);
      }
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error("Failed to read geography ZIP: {}", zipPath, e);
    }
  }

  private static void discoverNamespaces(Path root, boolean skipIfExists) {
    try (Stream<Path> namespaces = Files.list(root)) {
      namespaces
        .filter(Files::isDirectory)
        .forEach(namespacePath -> {
          String namespace = namespacePath.getFileName().toString();
          if (namespace.startsWith(".") || namespace.isEmpty()) {
            return;
          }
          // ZIP roots may expose a single leading slash entry; skip empty names.
          loadNamespace(namespacePath, namespace, skipIfExists);
        });
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error("Failed to list geography namespaces", e);
    }
  }

  private static void loadNamespace(
    Path namespacePath,
    String namespace,
    boolean skipIfExists
  ) {
    loadKind(namespacePath, namespace, GeographyKind.CONTINENT, skipIfExists);
    loadKind(namespacePath, namespace, GeographyKind.REGION, skipIfExists);
    loadKind(namespacePath, namespace, GeographyKind.SUBREGION, skipIfExists);
    loadKind(namespacePath, namespace, GeographyKind.WAYPOINT, skipIfExists);
  }

  private static void loadKind(
    Path namespacePath,
    String namespace,
    GeographyKind kind,
    boolean skipIfExists
  ) {
    String folderName =
      switch (kind) {
        case CONTINENT -> "continents";
        case REGION -> "regions";
        case SUBREGION -> "subregions";
        case WAYPOINT -> "waypoints";
      };
    Path folder = namespacePath.resolve(folderName);
    if (!Files.isDirectory(folder)) {
      return;
    }
    try (Stream<Path> files = Files.list(folder)) {
      files
        .filter(path -> path.getFileName().toString().endsWith(".json"))
        .forEach(path -> loadFile(namespace, kind, path, skipIfExists));
    } catch (IOException e) {
      TFCRealWorld.LOGGER.error(
        "Failed to list geography folder: {}",
        folder,
        e
      );
    }
  }

  private static void loadFile(
    String namespace,
    GeographyKind kind,
    Path path,
    boolean skipIfExists
  ) {
    String fileName = path.getFileName().toString();
    String slug = fileName.substring(0, fileName.length() - ".json".length());
    String ref = kind == GeographyKind.WAYPOINT
      ? namespace + ":" + slug
      : namespace + ":" + kind.path() + "/" + slug;
    String key = normalizeRef(ref);
    if (skipIfExists && NODES.containsKey(key)) {
      return;
    }
    try (InputStream stream = Files.newInputStream(path)) {
      JsonObject json = GSON.fromJson(
        new InputStreamReader(stream),
        JsonObject.class
      );
      if (json == null) {
        return;
      }
      NODES.put(key, parseNode(kind, namespace, slug, json));
    } catch (Exception e) {
      TFCRealWorld.LOGGER.error("Failed to load geography node: {}", path, e);
    }
  }

  private static GeographyNode parseNode(
    GeographyKind kind,
    String namespace,
    String slug,
    JsonObject json
  ) {
    String parentRef = null;
    if (json.has("parent") && json.get("parent").isJsonPrimitive()) {
      parentRef = json.get("parent").getAsString();
    } else if (json.has("continent")) {
      parentRef =
        namespace + ":continent/" + json.get("continent").getAsString();
    } else if (json.has("region")) {
      parentRef = namespace + ":region/" + json.get("region").getAsString();
    } else if (json.has("subregion")) {
      parentRef =
        namespace + ":subregion/" + json.get("subregion").getAsString();
    }

    Map<String, String> titleLang = readLangMap(json, "title_lang");
    Map<String, String> subtitleLang = readLangMap(json, "subtitle_lang");
    Map<String, String> descriptionLang = readLangMap(json, "description_lang");

    Double latitude = json.has("latitude")
      ? json.get("latitude").getAsDouble()
      : null;
    Double longitude = json.has("longitude")
      ? json.get("longitude").getAsDouble()
      : null;
    boolean capital =
      json.has("capital") && json.get("capital").getAsInt() != 0;

    return new GeographyNode(
      kind,
      namespace.toLowerCase(Locale.ROOT),
      slug.toLowerCase(Locale.ROOT),
      parentRef != null ? parentRef.toLowerCase(Locale.ROOT) : null,
      titleLang,
      subtitleLang,
      descriptionLang,
      latitude,
      longitude,
      capital
    );
  }

  private static Map<String, String> readLangMap(
    JsonObject json,
    String field
  ) {
    Map<String, String> result = new HashMap<>();
    if (!json.has(field) || !json.get(field).isJsonObject()) {
      return result;
    }
    JsonObject langObj = json.getAsJsonObject(field);
    for (Map.Entry<String, JsonElement> entry : langObj.entrySet()) {
      if (entry.getValue().isJsonPrimitive()) {
        result.put(
          entry.getKey().toLowerCase(Locale.ROOT),
          entry.getValue().getAsString()
        );
      }
    }
    return result;
  }
}
