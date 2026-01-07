package net.yazloysasha.tfcrealworld.types;

import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

public record CachedSpawnCenter(
  TFCRealWorldConfig.SpawnMode mode,
  long seed,
  int[] coords
) {}
