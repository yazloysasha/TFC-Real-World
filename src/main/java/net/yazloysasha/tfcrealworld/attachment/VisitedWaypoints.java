package net.yazloysasha.tfcrealworld.attachment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.yazloysasha.tfcrealworld.TFCRealWorld;

/**
 * Player-persisted map of waypoint ref → discovery epoch millis. Kept in the
 * player's persisted Forge data, which outlives death and dimension change.
 */
public final class VisitedWaypoints {

  private static final String KEY = TFCRealWorld.MOD_ID + ":visited_waypoints";

  /** Loaded data of the players online, by player object. */
  private static final Map<ServerPlayer, VisitedWaypoints> LOADED =
    Collections.synchronizedMap(new WeakHashMap<>());

  private final Map<String, Long> visited = new HashMap<>();

  public static VisitedWaypoints of(ServerPlayer player) {
    return LOADED.computeIfAbsent(player, VisitedWaypoints::load);
  }

  private static VisitedWaypoints load(ServerPlayer player) {
    final VisitedWaypoints data = new VisitedWaypoints();
    final ListTag list = player
      .getPersistentData()
      .getCompound(Player.PERSISTED_NBT_TAG)
      .getCompound(KEY)
      .getList("entries", Tag.TAG_COMPOUND);
    for (int i = 0; i < list.size(); i++) {
      final CompoundTag entry = list.getCompound(i);
      data.visited.put(
        entry.getString("ref").toLowerCase(),
        entry.getLong("at")
      );
    }
    return data;
  }

  public boolean isVisited(String ref) {
    return visited.containsKey(ref.toLowerCase());
  }

  /** @return true if newly marked */
  public boolean markVisited(
    ServerPlayer player,
    String ref,
    long epochMillis
  ) {
    String key = ref.toLowerCase();
    if (visited.containsKey(key)) {
      return false;
    }
    visited.put(key, epochMillis);
    save(player);
    return true;
  }

  public Map<String, Long> asMap() {
    return Collections.unmodifiableMap(visited);
  }

  private void save(ServerPlayer player) {
    final ListTag list = new ListTag();
    for (Map.Entry<String, Long> e : visited.entrySet()) {
      final CompoundTag entry = new CompoundTag();
      entry.putString("ref", e.getKey());
      entry.putLong("at", e.getValue());
      list.add(entry);
    }
    final CompoundTag tag = new CompoundTag();
    tag.put("entries", list);
    final CompoundTag root = player.getPersistentData();
    final CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
    persisted.put(KEY, tag);
    root.put(Player.PERSISTED_NBT_TAG, persisted);
  }
}
