package net.yazloysasha.tfcrealworld.types;

public record CachedSolarCalculation(
  int x,
  int z,
  int horizontalTileSize,
  int verticalTileSize,
  double[] geoCoords
) {}
