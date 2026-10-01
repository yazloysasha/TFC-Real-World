package net.yazloysasha.tfcrealworld.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import net.dries007.tfc.world.layer.TFCLayers;

/**
 * Biome layer ids declared by TFC in {@link TFCLayers}. Constants that mixins
 * merge into the class ({@code tfcrealworld$...}) are not biomes and would
 * otherwise shadow the names of biomes that share their int value.
 */
public final class TfcBiomeIds {

  private static final Map<Integer, String> NAMES = scan();

  private TfcBiomeIds() {}

  public static Map<Integer, String> names() {
    return NAMES;
  }

  public static String name(int biome) {
    return NAMES.getOrDefault(biome, "UNKNOWN_BIOME_" + biome);
  }

  private static Map<Integer, String> scan() {
    final Map<Integer, String> names = new LinkedHashMap<>();
    for (final Field field : TFCLayers.class.getDeclaredFields()) {
      final int modifiers = field.getModifiers();
      if (
        !Modifier.isStatic(modifiers) ||
        !Modifier.isFinal(modifiers) ||
        field.getType() != int.class ||
        field.getName().contains("$")
      ) {
        continue;
      }
      try {
        field.setAccessible(true);
        names.putIfAbsent(field.getInt(null), field.getName());
      } catch (IllegalAccessException e) {
        throw new IllegalStateException(e);
      }
    }
    return Map.copyOf(names);
  }
}
