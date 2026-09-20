package net.yazloysasha.tfcrealworld.world.region;

import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

/**
 * Shared edge falloff for finite continents (matches TFC 1.21.1):
 * {@code FINITE_CONTINENTS == true} → edge falloff; {@code false} → {@code 1f}.
 */
public final class FiniteContinentFactor {

  private FiniteContinentFactor() {}

  public static float atGrid(int gridX, int gridZ) {
    if (!TFCRealWorldConfig.FINITE_CONTINENTS.get()) {
      return 1f;
    }
    int scaleX = TFCRealWorldConfig.HORIZONTAL_SCALE.get();
    int scaleZ = TFCRealWorldConfig.VERTICAL_SCALE.get();
    float blockX = RegionCoords.gridToBlock(gridX);
    float blockZ = RegionCoords.gridToBlock(gridZ);
    float multiplier = TFCRealWorldConfig.CONTINENT_FROM_MAP.get()
      ? 1.01f
      : 1.2f;
    float factorX = scaleX == 0
      ? 1f
      : Mth.clampedMap(Math.abs(blockX), scaleX, multiplier * scaleX, 1, 0);
    float factorZ = scaleZ == 0
      ? 1f
      : Mth.clampedMap(Math.abs(blockZ), scaleZ, multiplier * scaleZ, 1, 0);
    return Math.min(factorX, factorZ);
  }
}
