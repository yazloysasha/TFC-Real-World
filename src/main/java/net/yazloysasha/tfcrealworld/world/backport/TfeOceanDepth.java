package net.yazloysasha.tfcrealworld.world.backport;

import com.newterraearth.tfe.world.region.NTEPointAccess;
import com.newterraearth.tfe.world.region.NTERegionGeneratorAccess;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.AltitudeNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGAltitudeNoise;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;

public final class TfeOceanDepth {

  private static final double CONTINENTAL_SHELF_THRESHOLD = 3.3;

  private TfeOceanDepth() {}

  public static void prepareOceanForBarrierIslands(
    Region region,
    RegionGenerator generator
  ) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }
    applyFromAltitudeMap(region, generator);
    applyContinentalShelf(region, generator);
  }

  public static void applyFromAltitudeMap(
    Region region,
    RegionGenerator generator
  ) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }
    PNGAltitudeNoise noise = AltitudeNoiseRegistry.get(generator);
    if (noise == null) {
      noise = new PNGAltitudeNoise(
        TFCRealWorldConfig.HORIZONTAL_SCALE.get(),
        TFCRealWorldConfig.VERTICAL_SCALE.get()
      );
      AltitudeNoiseRegistry.register(generator, noise);
    }
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      final NTEPointAccess access = (NTEPointAccess) point;
      if (point.land()) {
        access.nte$setOceanDepth((byte) 0);
        continue;
      }
      if (access.nte$getOceanDepth() == PNGAltitudeNoise.REEF_OCEAN_DEPTH) {
        continue;
      }
      access.nte$setOceanDepth(
        PNGAltitudeNoise.tfeDepthFromRaw(
          noise.getRawOceanDepth(
            RegionCoords.gridX(region, index),
            RegionCoords.gridZ(region, index)
          )
        )
      );
    }
  }

  private static void applyContinentalShelf(
    Region region,
    RegionGenerator generator
  ) {
    final boolean mapTectonics = MapTectonics.isActive(generator);
    final NTERegionGeneratorAccess generatorAccess =
      (NTERegionGeneratorAccess) generator;
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null || point.land()) {
        continue;
      }
      final NTEPointAccess access = (NTEPointAccess) point;
      final int depth = Byte.toUnsignedInt(access.nte$getOceanDepth());
      if (depth == PNGAltitudeNoise.REEF_OCEAN_DEPTH || depth >= 5) {
        continue;
      }
      final int gridX = RegionCoords.gridX(region, index);
      final int gridZ = RegionCoords.gridZ(region, index);
      final double tectonicFeatures = mapTectonics
        ? MapTectonics.continentRiftAdjustment(
          (float) access.nte$getDivergence()
        )
        : 0;
      final double continent =
        (generator.continentNoise.noise(gridX, gridZ) + tectonicFeatures) *
        generatorAccess.nte$continentFactor(gridX, gridZ);
      if (continent > CONTINENTAL_SHELF_THRESHOLD) {
        access.nte$setOceanDepth(PNGAltitudeNoise.SHELF_OCEAN_DEPTH);
      }
    }
  }
}
