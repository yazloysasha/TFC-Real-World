package net.yazloysasha.tfcrealworld.attachment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Player-persisted map of waypoint ref → discovery epoch millis.
 */
public final class VisitedWaypoints implements INBTSerializable<CompoundTag> {

  private final Map<String, Long> visited = new HashMap<>();

  public boolean isVisited(String ref) {
    return visited.containsKey(ref.toLowerCase());
  }

  public Long getVisitedAt(String ref) {
    return visited.get(ref.toLowerCase());
  }

  /** @return true if newly marked */
  public boolean markVisited(String ref, long epochMillis) {
    String key = ref.toLowerCase();
    if (visited.containsKey(key)) {
      return false;
    }
    visited.put(key, epochMillis);
    return true;
  }

  public Map<String, Long> asMap() {
    return Collections.unmodifiableMap(visited);
  }

  public void replaceAll(Map<String, Long> data) {
    visited.clear();
    for (Map.Entry<String, Long> e : data.entrySet()) {
      visited.put(e.getKey().toLowerCase(), e.getValue());
    }
  }

  @Override
  public CompoundTag serializeNBT(HolderLookup.Provider provider) {
    CompoundTag tag = new CompoundTag();
    ListTag list = new ListTag();
    for (Map.Entry<String, Long> e : visited.entrySet()) {
      CompoundTag entry = new CompoundTag();
      entry.putString("ref", e.getKey());
      entry.putLong("at", e.getValue());
      list.add(entry);
    }
    tag.put("entries", list);
    return tag;
  }

  @Override
  public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
    visited.clear();
    ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
    for (int i = 0; i < list.size(); i++) {
      CompoundTag entry = list.getCompound(i);
      visited.put(entry.getString("ref").toLowerCase(), entry.getLong("at"));
    }
  }
}
