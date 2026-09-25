package net.yazloysasha.tfcrealworld.util.geography;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public record GeographyNode(
  GeographyKind kind,
  String namespace,
  String slug,
  @Nullable String parentRef,
  Map<String, String> titleLang,
  Map<String, String> subtitleLang,
  Map<String, String> descriptionLang,
  @Nullable Double latitude,
  @Nullable Double longitude,
  boolean capital
) {
  public GeographyNode {
    titleLang = Collections.unmodifiableMap(new HashMap<>(titleLang));
    subtitleLang = Collections.unmodifiableMap(new HashMap<>(subtitleLang));
    descriptionLang = Collections.unmodifiableMap(
      new HashMap<>(descriptionLang)
    );
  }

  public String ref() {
    if (kind == GeographyKind.WAYPOINT) {
      return namespace + ":" + slug;
    }
    return namespace + ":" + kind.path() + "/" + slug;
  }

  public String getTitle(String languageCode) {
    return pick(titleLang, languageCode, slug);
  }

  /**
   * Empty when no subtitle_lang entry (non-capitals / missing).
   */
  public String getSubtitle(String languageCode) {
    if (subtitleLang == null || subtitleLang.isEmpty()) {
      return "";
    }
    String key = languageCode != null ? languageCode.toLowerCase() : "en_us";
    String v = subtitleLang.getOrDefault(
      key,
      subtitleLang.getOrDefault("en_us", "")
    );
    return v != null ? v : "";
  }

  public boolean hasSubtitle(String languageCode) {
    String s = getSubtitle(languageCode);
    return s != null && !s.isBlank();
  }

  public String getDescription(String languageCode) {
    return pick(descriptionLang, languageCode, slug);
  }

  private static String pick(
    Map<String, String> lang,
    String languageCode,
    String fallback
  ) {
    if (lang == null || lang.isEmpty()) {
      return fallback;
    }
    String key = languageCode != null ? languageCode.toLowerCase() : "en_us";
    return lang.getOrDefault(key, lang.getOrDefault("en_us", fallback));
  }
}
