package net.yazloysasha.tfcrealworld.world.backport;

import com.newterraearth.tfe.world.region.NTEPointAccess;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.util.registry.DivergenceNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGDivergenceNoise;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;

/**
 * 1.21.1 {@code AnnotateBoundaryTypesMixin} + ChooseBiomes HEAD zeroing of
 * non-core land rifts. TFE ChooseBiomes still treats
 * {@code distanceToEdge < 3 && divergence > 0} as a plate rift.
 */
public final class TfeMapDivergence {

  private TfeMapDivergence() {}

  public static void stampFromMap(RegionGenerator.Context context) {
    stampFromMap(context, false);
  }

  public static void stampFromMapAndSuppressDefaultRifts(
    RegionGenerator.Context context
  ) {
    stampFromMap(context, true);
  }

  private static void stampFromMap(
    RegionGenerator.Context context,
    boolean suppressDefaultRifts
  ) {
    if (!MapTectonics.isActive(context.generator())) {
      return;
    }
    final PNGDivergenceNoise divergenceNoise = DivergenceNoiseRegistry.get(
      context.generator()
    );
    if (divergenceNoise == null) {
      return;
    }
    final Region region = context.region;
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      final int x = RegionCoords.gridX(region, index);
      final int z = RegionCoords.gridZ(region, index);
      float divergence = divergenceNoise.getDivergence(x, z);
      if (
        suppressDefaultRifts &&
        point.land() &&
        point.distanceToEdge < 3 &&
        !MapTectonics.isLandRiftCore(divergenceNoise, x, z, divergence) &&
        divergence > 0
      ) {
        divergence = 0f;
      }
      ((NTEPointAccess) point).nte$setDivergence(divergence);
    }
  }
}
