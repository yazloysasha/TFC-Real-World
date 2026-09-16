package net.yazloysasha.tfcrealworld.world.region.calculator;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.AltitudeNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGAltitudeNoise;

public class AltitudeCalculator extends RegionPointCalculator {

  @Override
  public void calculate(Region region, RegionGenerator generator) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }

    PNGAltitudeNoise altitudeNoise = AltitudeNoiseRegistry.get(generator);
    if (altitudeNoise == null) {
      altitudeNoise = new PNGAltitudeNoise(
        TFCRealWorldConfig.HORIZONTAL_SCALE.get(),
        TFCRealWorldConfig.VERTICAL_SCALE.get()
      );
      AltitudeNoiseRegistry.register(generator, altitudeNoise);
    }

    final PNGAltitudeNoise noise = altitudeNoise;
    forEachPoint(region, point -> {
      if (point.land()) {
        point.baseLandHeight = noise.getBaseLandHeight(point.x, point.z);
      } else {
        point.oceanDepth = noise.getBaseOceanDepth(point.x, point.z);
      }
    });
  }
}
