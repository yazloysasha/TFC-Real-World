package net.yazloysasha.tfcrealworld.types;

public record CachedSolarCalculation(
  int z,
  int horizontalTileSize,
  int verticalTileSize,
  double[] geoCoords
) {}
