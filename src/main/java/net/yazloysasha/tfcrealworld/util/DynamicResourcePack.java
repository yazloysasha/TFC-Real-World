package net.yazloysasha.tfcrealworld.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

public class DynamicResourcePack extends BaseDynamicPack {

  private static final String LANG_PATH_PREFIX = "lang/";
  private static final String LANG_RESOURCE_PATH_PREFIX = "/assets/tfc/lang/";

  private static List<String> languageCodesCache = null;
  private static final Map<String, String> langJsonCache = new HashMap<>();

  public DynamicResourcePack(PackLocationInfo locationInfo) {
    super(locationInfo);
  }

  private static List<String> getLanguageCodes() {
    if (languageCodesCache != null) {
      return languageCodesCache;
    }

    List<String> codes = new ArrayList<>();

    String[] possibleCodes = {
      "en_us",
      "ru_ru",
      "de_de",
      "es_es",
      "pt_br",
      "zh_cn",
      "zh_tw",
      "zh_hk",
      "ja_jp",
      "ko_kr",
      "pl_pl",
      "tr_tr",
      "uk_ua",
    };

    for (String code : possibleCodes) {
      String resourcePath = LANG_RESOURCE_PATH_PREFIX + code + ".json";
      try (
        InputStream stream =
          DynamicResourcePack.class.getResourceAsStream(resourcePath)
      ) {
        if (stream != null) {
          codes.add(code);
        }
      } catch (Exception ignored) {}
    }

    if (codes.isEmpty()) {
      codes.add("en_us");
    }

    languageCodesCache = codes;
    return codes;
  }

  private String getLangJson(String langCode) {
    synchronized (langJsonCache) {
      String cached = langJsonCache.get(langCode);
      if (cached != null) {
        return cached;
      }
    }

    String resourcePath = LANG_RESOURCE_PATH_PREFIX + langCode + ".json";
    InputStream resourceStream =
      DynamicResourcePack.class.getResourceAsStream(resourcePath);

    if (resourceStream == null) {
      resourcePath = LANG_RESOURCE_PATH_PREFIX + "en_us.json";
      resourceStream = DynamicResourcePack.class.getResourceAsStream(
          resourcePath
        );
      if (resourceStream == null) {
        throw new IllegalStateException(
          "Language template not found at: " + resourcePath
        );
      }
    }

    final InputStream finalStream = resourceStream;
    try (finalStream) {
      String template = new String(
        finalStream.readAllBytes(),
        StandardCharsets.UTF_8
      );

      int[] scaleValues = getWorldScaleValues();
      int halfScale = scaleValues[0];
      int negativeHalfScale = scaleValues[1];

      String json = template
        .replace(
          "{WORLD_SCALE_HALF_FORMATTED}",
          formatNumberWithCommas(halfScale)
        )
        .replace(
          "{WORLD_SCALE_NEGATIVE_HALF_FORMATTED}",
          formatNumberWithCommas(negativeHalfScale)
        );

      synchronized (langJsonCache) {
        langJsonCache.put(langCode, json);
      }
      return json;
    } catch (Exception e) {
      throw new RuntimeException(
        "Failed to load language template for " +
        langCode +
        ": " +
        e.getMessage(),
        e
      );
    }
  }

  @Override
  public @Nullable IoSupplier<InputStream> getResource(
    PackType type,
    ResourceLocation location
  ) {
    String path = location.getPath();

    if (
      type == PackType.CLIENT_RESOURCES &&
      isCorrectNamespace(location.getNamespace()) &&
      path.startsWith(LANG_PATH_PREFIX) &&
      path.endsWith(".json")
    ) {
      String langCode = path.substring(
        LANG_PATH_PREFIX.length(),
        path.length() - 5
      );

      String json = getLangJson(langCode);
      return createJsonInputStream(json);
    }

    return null;
  }

  @Override
  public void listResources(
    PackType type,
    String namespace,
    String path,
    ResourceOutput output
  ) {
    if (
      type == PackType.CLIENT_RESOURCES &&
      isCorrectNamespace(namespace) &&
      path.equals("lang")
    ) {
      for (String langCode : getLanguageCodes()) {
        ResourceLocation langLocation = ResourceLocation.fromNamespaceAndPath(
          TFC_NAMESPACE,
          "lang/" + langCode + ".json"
        );
        IoSupplier<InputStream> resource = getResource(type, langLocation);
        if (resource != null) {
          output.accept(langLocation, resource);
        }
      }
    }
  }

  @Override
  protected String getPackDisplayName() {
    return "Resources for fixes";
  }

  @Override
  protected PackType getSupportedPackType() {
    return PackType.CLIENT_RESOURCES;
  }

  @Override
  protected int getPackFormat() {
    return 34;
  }
}
