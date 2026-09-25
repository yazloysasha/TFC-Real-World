package net.yazloysasha.tfcrealworld.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public final class ClientVisitedWaypoints {

  private static Map<String, Long> visited = Map.of();

  private ClientVisitedWaypoints() {}

  public static void replaceAll(Map<String, Long> data) {
    Map<String, Long> next = new HashMap<>();
    for (Map.Entry<String, Long> e : data.entrySet()) {
      next.put(e.getKey().toLowerCase(), e.getValue());
    }
    visited = Collections.unmodifiableMap(next);
  }

  public static void put(String ref, long at) {
    Map<String, Long> next = new HashMap<>(visited);
    next.put(ref.toLowerCase(), at);
    visited = Collections.unmodifiableMap(next);
  }

  public static boolean isVisited(String ref) {
    return visited.containsKey(ref.toLowerCase());
  }

  public static @Nullable Long getVisitedAt(String ref) {
    return visited.get(ref.toLowerCase());
  }

  public static Map<String, Long> asMap() {
    return visited;
  }

  public static void clear() {
    visited = Map.of();
  }
}
