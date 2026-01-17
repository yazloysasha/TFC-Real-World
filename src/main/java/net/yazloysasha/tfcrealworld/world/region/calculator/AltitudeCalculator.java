package net.yazloysasha.tfcrealworld.world.region.calculator;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.AltitudeNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGAltitudeNoise;

/**
 * Calculator for land height and water depth based on altitude map.
 */
public class AltitudeCalculator extends RegionPointCalculator {

  @Override
  public void calculate(Region region, RegionGenerator generator) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }

    final PNGAltitudeNoise altitudeNoise = AltitudeNoiseRegistry.get(generator);
    if (altitudeNoise == null) {
      return;
    }

    final int minX = region.minX();
    final int minZ = region.minZ();
    final int sizeX = region.sizeX();

    forEachPoint(region, point -> {
      final int index = findPointIndex(region.data(), point);
      if (index == -1) return;

      final int localX = index % sizeX;
      final int localZ = index / sizeX;
      final int gridX = minX + localX;
      final int gridZ = minZ + localZ;

      if (point.land()) {
        point.baseLandHeight = altitudeNoise.getBaseLandHeight(
          (double) gridX,
          (double) gridZ
        );
      } else {
        point.baseOceanDepth = altitudeNoise.getBaseOceanDepth(
          (double) gridX,
          (double) gridZ
        );
      }
    });
  }

  private int findPointIndex(Region.Point[] data, Region.Point point) {
    for (int i = 0; i < data.length; i++) {
      if (data[i] == point) {
        return i;
      }
    }
    return -1;
  }
}
