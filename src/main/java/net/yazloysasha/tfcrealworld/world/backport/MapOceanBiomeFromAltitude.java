package net.yazloysasha.tfcrealworld.world.backport;

import static com.newterraearth.tfe.world.NTELayerIds.DEEP_OCEAN_ATOLLS;
import static com.newterraearth.tfe.world.NTELayerIds.OCEANIC_VOLCANIC_ARC;
import static com.newterraearth.tfe.world.NTELayerIds.OCEAN_ATOLLS;
import static com.newterraearth.tfe.world.NTELayerIds.OCEAN_RIDGE;
import static net.dries007.tfc.world.layer.TFCLayers.DEEP_OCEAN;
import static net.dries007.tfc.world.layer.TFCLayers.DEEP_OCEAN_TRENCH;
import static net.dries007.tfc.world.layer.TFCLayers.OCEAN;
import static net.dries007.tfc.world.layer.TFCLayers.OCEAN_REEF;

import com.newterraearth.tfe.world.region.NTEPointAccess;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGAltitudeNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGDivergenceNoise;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;

public final class MapOceanBiomeFromAltitude {

  private static final int TRENCH_SHELF_INFLUENCE_RADIUS = 2;

  private MapOceanBiomeFromAltitude() {}

  public static void assign(
    Region.Point point,
    int gridX,
    int gridZ,
    NTEPointAccess access,
    PNGDivergenceNoise divergenceNoise,
    Area blobArea,
    long rngSeed,
    int[] subductionShelfBiomes
  ) {
    final int rawDepth = mapAssignRawDepth(point, access);
    if (rawDepth >= PNGAltitudeNoise.MAP_OCEAN_TRENCH_RAW_DEPTH) {
      point.biome = DEEP_OCEAN_TRENCH;
      return;
    }

    point.biome = baseBiomeForMapDepth(point, access, rawDepth);

    if (divergenceNoise == null) {
      return;
    }

    if (MapTectonics.isNearOceanRidge((float) access.nte$getDivergence())) {
      point.biome = OCEAN_RIDGE;
      return;
    }

    if (
      rawDepth != PNGAltitudeNoise.REEF_OCEAN_DEPTH &&
      isSubductionShelf(point, access, divergenceNoise, gridX, gridZ)
    ) {
      final int areaSeed = blobArea.get(gridX, gridZ);
      point.biome = ChooseBiomesSupport.randomSeededFrom(
        rngSeed,
        areaSeed,
        subductionShelfBiomes
      );
    }
  }

  public static int mapAssignRawDepth(
    Region.Point point,
    NTEPointAccess access
  ) {
    if (
      Byte.toUnsignedInt(point.baseOceanDepth) ==
        PNGAltitudeNoise.REEF_OCEAN_DEPTH ||
      access.nte$getOceanDepth() == PNGAltitudeNoise.REEF_OCEAN_DEPTH
    ) {
      return PNGAltitudeNoise.REEF_OCEAN_DEPTH;
    }
    return Byte.toUnsignedInt(
      PNGAltitudeNoise.normalizeMapOceanDepth(point.baseOceanDepth)
    );
  }

  public static int baseBiomeForMapDepth(
    Region.Point point,
    NTEPointAccess access,
    int rawDepth
  ) {
    if (
      rawDepth == PNGAltitudeNoise.REEF_OCEAN_DEPTH ||
      access.nte$getOceanDepth() == PNGAltitudeNoise.REEF_OCEAN_DEPTH
    ) {
      return access.nte$isVolcanic() ? OCEANIC_VOLCANIC_ARC : OCEAN_REEF;
    }

    final int depthBucket = Byte.toUnsignedInt(
      PNGAltitudeNoise.bucketFromRawOceanDepth(rawDepth)
    );
    if (depthBucket <= 2) {
      return (
          point.temperature > 12 &&
          Byte.toUnsignedInt(access.nte$getDistanceToLand()) > 4
        )
        ? OCEAN_ATOLLS
        : OCEAN;
    }
    if (depthBucket == PNGAltitudeNoise.ABYSSAL_OCEAN_DEPTH) {
      return DEEP_OCEAN;
    }
    if (depthBucket >= 4) {
      return (
          point.temperature > 12 &&
          Byte.toUnsignedInt(access.nte$getDistanceToLand()) > 3
        )
        ? DEEP_OCEAN_ATOLLS
        : DEEP_OCEAN;
    }
    return OCEAN;
  }

  private static boolean isSubductionShelf(
    Region.Point point,
    NTEPointAccess access,
    PNGDivergenceNoise divergenceNoise,
    int gridX,
    int gridZ
  ) {
    final int rawDepth = mapAssignRawDepth(point, access);
    if (
      rawDepth == PNGAltitudeNoise.REEF_OCEAN_DEPTH ||
      PNGAltitudeNoise.bucketFromRawOceanDepth(rawDepth) != 2
    ) {
      return false;
    }
    if (access.nte$getDivergence() >= 0d) {
      return false;
    }
    if (
      !MapTectonics.isNearTrenchInfluence(
        divergenceNoise,
        gridX,
        gridZ,
        TRENCH_SHELF_INFLUENCE_RADIUS
      )
    ) {
      return false;
    }
    final int distanceToLand = Byte.toUnsignedInt(
      access.nte$getDistanceToLand()
    );
    return distanceToLand > 1 && distanceToLand < 8;
  }
}
