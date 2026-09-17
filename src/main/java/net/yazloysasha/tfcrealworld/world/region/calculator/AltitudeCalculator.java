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

    final PNGAltitudeNoise noise = resolveNoise(generator);
    final Region.Point[] data = region.data();
    final int minX = region.minX();
    final int minZ = region.minZ();
    final int sizeX = region.sizeX();

    for (int i = 0; i < data.length; i++) {
      final Region.Point point = data[i];
      if (point == null) {
        continue;
      }
      final int gridX = minX + (i % sizeX);
      final int gridZ = minZ + (i / sizeX);
      if (point.land()) {
        point.baseLandHeight = noise.getBaseLandHeight(gridX, gridZ);
      } else {
        point.baseOceanDepth = noise.getBaseOceanDepth(gridX, gridZ);
      }
    }
  }

  private static PNGAltitudeNoise resolveNoise(RegionGenerator generator) {
    PNGAltitudeNoise altitudeNoise = AltitudeNoiseRegistry.get(generator);
    if (altitudeNoise == null) {
      altitudeNoise = new PNGAltitudeNoise(
        TFCRealWorldConfig.HORIZONTAL_SCALE.get(),
        TFCRealWorldConfig.VERTICAL_SCALE.get()
      );
      AltitudeNoiseRegistry.register(generator, altitudeNoise);
    }
    return altitudeNoise;
  }
}
