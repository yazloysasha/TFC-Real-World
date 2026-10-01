package net.yazloysasha.tfcrealworld.world.tectonics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.Boundary;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.LandRelief;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.Volcanism;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.WaterDepth;
import org.jetbrains.annotations.Nullable;

/**
 * {@code tectonics.json}: one {@link TectonicClass} per {@code tectonics.png}
 * palette index, in palette order.
 *
 * <pre>
 * [
 *   { "water": "reef" },
 *   { "boundary": "divergent", "land": "upland", "volcanism": "rift" },
 *   { "land": "mountain", "coast": true, "hotspot": 2 }
 * ]
 * </pre>
 * Omitted fields use {@link TectonicClass#DEFAULT} values.
 */
public final class TectonicLegend {

  public static final int SIZE = 256;

  private final TectonicClass[] classes;

  private TectonicLegend(TectonicClass[] classes) {
    this.classes = classes;
  }

  @Nullable
  public TectonicClass get(int index) {
    return index < classes.length ? classes[index] : null;
  }

  public static TectonicLegend read(InputStream stream) throws IOException {
    try (
      Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)
    ) {
      return parse(JsonParser.parseReader(reader).getAsJsonArray());
    }
  }

  static TectonicLegend parse(JsonArray entries) {
    if (entries.size() > SIZE) {
      throw new IllegalArgumentException(
        "Tectonics legend has " + entries.size() + " classes, at most " + SIZE
      );
    }
    final TectonicClass[] classes = new TectonicClass[entries.size()];
    for (int index = 0; index < classes.length; index++) {
      classes[index] = parseClass(entries.get(index).getAsJsonObject(), index);
    }
    return new TectonicLegend(classes);
  }

  private static TectonicClass parseClass(JsonObject json, int index) {
    final TectonicClass defaults = TectonicClass.DEFAULT;
    final int hotspot = json.has("hotspot")
      ? json.get("hotspot").getAsInt()
      : 0;
    if (hotspot < 0 || hotspot > 4) {
      throw new IllegalArgumentException(
        "Tectonics legend class " + index + " has hotspot age " + hotspot
      );
    }
    return new TectonicClass(
      TectonicClass.parseEnum(
        Boundary.class,
        string(json, "boundary"),
        defaults.boundary(),
        "boundary",
        index
      ),
      TectonicClass.parseEnum(
        LandRelief.class,
        string(json, "land"),
        defaults.land(),
        "land",
        index
      ),
      TectonicClass.parseEnum(
        WaterDepth.class,
        string(json, "water"),
        defaults.water(),
        "water",
        index
      ),
      TectonicClass.parseEnum(
        Volcanism.class,
        string(json, "volcanism"),
        defaults.volcanism(),
        "volcanism",
        index
      ),
      json.has("coast") && json.get("coast").getAsBoolean(),
      (byte) hotspot
    );
  }

  @Nullable
  private static String string(JsonObject json, String key) {
    final JsonElement value = json.get(key);
    return value == null || value.isJsonNull() ? null : value.getAsString();
  }
}
