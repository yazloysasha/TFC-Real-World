package net.yazloysasha.tfcrealworld.types;

public record CachedClassicCoords(
  double spawnCenterLongitude,
  double spawnCenterLatitude,
  int spawnCenterX,
  int spawnCenterZ,
  int horizontalScale,
  int verticalScale
) {}
