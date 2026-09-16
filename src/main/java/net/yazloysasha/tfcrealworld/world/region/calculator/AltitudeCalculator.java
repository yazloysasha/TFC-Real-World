package net.yazloysasha.tfcrealworld.world.region.calculator;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.AltitudeNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGAltitudeNoise;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;

public class AltitudeCalculator extends RegionPointCalculator {

  private static final double CONTINENTAL_SHELF_THRESHOLD = 3.3;

  public void prepareOceanForBarrierIslands(
    Region region,
    RegionGenerator generator
  ) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }
    applyOceanDepthsFromMap(region, generator);
    applyContinentalShelf(region, generator);
  }

  public void applyOceanDepthsFromMap(
    Region region,
    RegionGenerator generator
  ) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }
    final PNGAltitudeNoise noise = resolveNoise(generator);
    forEachPoint(region, point -> {
      if (!point.land()) {
        setMapOceanDepth(point, noise);
      }
    });
  }

  @Override
  public void calculate(Region region, RegionGenerator generator) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }

    final PNGAltitudeNoise noise = resolveNoise(generator);
    forEachPoint(region, point -> {
      if (point.land()) {
        point.baseLandHeight = noise.getBaseLandHeight(point.x, point.z);
      } else {
        setMapOceanDepth(point, noise);
      }
    });
  }

  private void applyContinentalShelf(Region region, RegionGenerator generator) {
    final boolean mapTectonics = MapTectonics.isActive(generator);
    forEachPoint(region, point -> {
      if (point.land()) {
        return;
      }
      final int depth = Byte.toUnsignedInt(point.oceanDepth);
      if (
        depth == PNGAltitudeNoise.REEF_OCEAN_DEPTH ||
        depth >= PNGAltitudeNoise.MAP_OCEAN_TRENCH_RAW_DEPTH
      ) {
        return;
      }
      final double tectonicFeatures = mapTectonics
        ? MapTectonics.continentRiftAdjustment(point.divergence)
        : 0;
      final double continent =
        (generator.continentNoise.noise(point.x, point.z) + tectonicFeatures) *
        generator.continentFactor(point);
      if (continent > CONTINENTAL_SHELF_THRESHOLD) {
        point.oceanDepth = PNGAltitudeNoise.SHELF_OCEAN_DEPTH;
      }
    });
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

  private static void setMapOceanDepth(
    Region.Point point,
    PNGAltitudeNoise noise
  ) {
    if (
      Byte.toUnsignedInt(point.oceanDepth) == PNGAltitudeNoise.REEF_OCEAN_DEPTH
    ) {
      return;
    }
    point.oceanDepth = PNGAltitudeNoise.normalizeMapOceanDepth(
      noise.getBaseOceanDepth(point.x, point.z)
    );
  }
}
