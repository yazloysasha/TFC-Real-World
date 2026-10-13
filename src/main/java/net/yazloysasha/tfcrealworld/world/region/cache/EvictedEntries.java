package net.yazloysasha.tfcrealworld.world.region.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * What TFC's region caches have been given, kept past their evictions. Each
 * of those caches holds one entry per slot: two regions that fall into the
 * same slot throw each other out, and a place whose chunks read both makes
 * each of them again on every read. An entry stays here until as many newer
 * ones have come as the store holds.
 */
public final class EvictedEntries<T> {

  private final ConcurrentHashMap<Long, T> entries = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<Long> order =
    new ConcurrentLinkedQueue<>();
  private final int capacity;

  public EvictedEntries(int capacity) {
    this.capacity = capacity;
  }

  @Nullable
  public T get(int x, int z) {
    return entries.get(ChunkPos.asLong(x, z));
  }

  public void put(int x, int z, T entry) {
    final Long key = ChunkPos.asLong(x, z);
    if (entries.put(key, entry) == null) {
      order.add(key);
      while (entries.size() > capacity) {
        final Long eldest = order.poll();
        if (eldest == null) {
          break;
        }
        entries.remove(eldest);
      }
    }
  }
}
