package net.yazloysasha.tfcrealworld.types;

public record CachedGeographicCoords(
  int spawnCenterX,
  int spawnCenterZ,
  int horizontalTileSize,
  int verticalTileSize,
  double longitude,
  double latitude
) {}
