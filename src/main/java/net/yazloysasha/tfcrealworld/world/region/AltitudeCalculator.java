package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.AltitudeNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.PNGAltitudeNoise;

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

    forEachPoint(
      region,
      point -> {
        if (point.land()) {
          point.baseLandHeight = altitudeNoise.getBaseLandHeight(
            (double) point.x,
            (double) point.z
          );
        } else {
          point.baseOceanDepth = altitudeNoise.getBaseOceanDepth(
            (double) point.x,
            (double) point.z
          );
        }
      }
    );
  }
}
