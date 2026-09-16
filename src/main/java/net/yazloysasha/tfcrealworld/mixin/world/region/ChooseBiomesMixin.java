package net.yazloysasha.tfcrealworld.mixin.world.region;

import static net.dries007.tfc.world.layer.TFCLayers.*;

import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.DivergenceNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGAltitudeNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGDivergenceNoise;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChooseBiomes.class, remap = false)
public class ChooseBiomesMixin {

  @Unique
  private static final int[] RIFT_VALLEY_BIOMES = {
    RIFT_VALLEY,
    RIFT_VALLEY,
    RIFT_VALLEY,
    RIFT_LAKE,
    RIFT_LAKE,
  };

  @Unique
  private static final int[] LAND_RIFT_SPAWN_ROLL = { 1, 0 };

  @Unique
  private static final int[] VOLCANIC_ARC_BIOMES = {
    VOLCANIC_OCEANIC_MOUNTAINS,
    VOLCANIC_OCEANIC_MOUNTAINS,
    VOLCANIC_OCEANIC_MOUNTAINS,
    VOLCANIC_ISLAND,
    OCEANIC_VOLCANIC_ARC,
  };

  @Inject(method = "apply", at = @At("HEAD"))
  private void tfcrealworld$prepareMapDivergenceForChooseBiomes(
    RegionGenerator.Context context,
    CallbackInfo ci
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
    for (final Region.Point point : context.region.points()) {
      float divergence = divergenceNoise.getDivergence(point.x, point.z);
      if (
        point.land() &&
        point.distanceToEdge < 3 &&
        !MapTectonics.isLandRiftCore(divergenceNoise, point.x, point.z) &&
        divergence > 0
      ) {
        divergence = 0f;
      }
      point.divergence = divergence;
    }
  }

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$applyMapBiomes(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    final RegionGenerator generator = context.generator();
    final boolean altitudeFromMap = TFCRealWorldConfig.ALTITUDE_FROM_MAP.get();
    final PNGDivergenceNoise divergenceNoise = MapTectonics.isActive(generator)
      ? DivergenceNoiseRegistry.get(generator)
      : null;

    if (divergenceNoise == null && !altitudeFromMap) {
      return;
    }

    final ChooseBiomesAccessor accessor = (ChooseBiomesAccessor) this;
    final Region region = context.region;
    final Area blobArea = divergenceNoise != null || altitudeFromMap
      ? context.generator().biomeArea.get()
      : null;
    final long rngSeed = context.random.nextLong();

    for (final Region.Point point : region.points()) {
      if (divergenceNoise != null) {
        point.divergence = divergenceNoise.getDivergence(point.x, point.z);
        if (
          point.land() &&
          !point.island() &&
          point.hotSpotAge == 0 &&
          MapTectonics.isLandRiftCore(divergenceNoise, point.x, point.z)
        ) {
          final int areaSeed = blobArea.get(point.x, point.z);
          if (
            accessor.tfcrealworld$invokeRandomSeededFrom(
              rngSeed,
              areaSeed,
              LAND_RIFT_SPAWN_ROLL
            ) ==
            1
          ) {
            if (point.distanceToOcean > 2) {
              point.biome = accessor.tfcrealworld$invokeRandomSeededFrom(
                rngSeed,
                areaSeed ^ 0x5f3759df,
                RIFT_VALLEY_BIOMES
              );
            } else {
              point.biome = RIFT_VALLEY;
            }
          }
        }
      }

      if (altitudeFromMap && !tfcrealworld$skipOceanBiome(point)) {
        tfcrealworld$assignMapOceanBiome(
          point,
          region,
          divergenceNoise,
          accessor,
          blobArea,
          rngSeed
        );
      }
    }
  }

  @Unique
  private static void tfcrealworld$assignMapOceanBiome(
    Region.Point point,
    Region region,
    PNGDivergenceNoise divergenceNoise,
    ChooseBiomesAccessor accessor,
    Area blobArea,
    long rngSeed
  ) {
    final int rawDepth = Byte.toUnsignedInt(point.oceanDepth);
    if (rawDepth >= PNGAltitudeNoise.MAP_OCEAN_TRENCH_RAW_DEPTH) {
      point.biome = DEEP_OCEAN_TRENCH;
      return;
    }

    final int areaSeed = blobArea.get(point.x, point.z);
    final int depthBucket = tfcrealworld$mapOceanDepthBucket(rawDepth);

    if (rawDepth == PNGAltitudeNoise.REEF_OCEAN_DEPTH) {
      point.biome = point.volcanic() ? OCEANIC_VOLCANIC_ARC : OCEAN_REEF;
    } else if (depthBucket <= 2) {
      point.biome = point.temperature > 12 && point.distanceToLand > 4
        ? OCEAN_ATOLLS
        : OCEAN;
    } else if (depthBucket == PNGAltitudeNoise.ABYSSAL_OCEAN_DEPTH) {
      point.biome = DEEP_OCEAN;
    } else if (depthBucket >= 4) {
      point.biome = point.temperature > 12 && point.distanceToLand > 3
        ? DEEP_OCEAN_ATOLLS
        : DEEP_OCEAN;
    } else {
      point.biome = OCEAN;
    }

    if (
      divergenceNoise != null &&
      MapTectonics.isNearOceanRidge(divergenceNoise, point.x, point.z)
    ) {
      point.biome = OCEAN_RIDGE;
    } else if (
      divergenceNoise != null &&
      depthBucket <= 2 &&
      tfcrealworld$mapVolcanicArcShelf(region, divergenceNoise, point)
    ) {
      point.biome = accessor.tfcrealworld$invokeRandomSeededFrom(
        rngSeed,
        areaSeed,
        VOLCANIC_ARC_BIOMES
      );
    }
  }

  @Unique
  private static int tfcrealworld$mapOceanDepthBucket(int rawDepth) {
    return rawDepth == PNGAltitudeNoise.REEF_OCEAN_DEPTH
      ? PNGAltitudeNoise.REEF_OCEAN_DEPTH
      : Byte.toUnsignedInt(PNGAltitudeNoise.bucketFromRawOceanDepth(rawDepth));
  }

  @Unique
  private static boolean tfcrealworld$mapVolcanicArcShelf(
    Region region,
    PNGDivergenceNoise divergenceNoise,
    Region.Point point
  ) {
    final int rawDepth = Byte.toUnsignedInt(point.oceanDepth);
    if (
      rawDepth == 0 || PNGAltitudeNoise.bucketFromRawOceanDepth(rawDepth) != 2
    ) {
      return false;
    }
    if (
      point.divergence < 0f ||
      MapTectonics.isNearTrench(divergenceNoise, point.x, point.z)
    ) {
      return true;
    }
    for (int dz = -3; dz <= 3; dz++) {
      for (int dx = -3; dx <= 3; dx++) {
        final Region.Point neighbor = region.atOffset(point.index, dx, dz);
        if (
          neighbor != null &&
          MapTectonics.isNearTrench(divergenceNoise, neighbor.x, neighbor.z)
        ) {
          return true;
        }
      }
    }
    return false;
  }

  @Unique
  private static boolean tfcrealworld$skipOceanBiome(Region.Point point) {
    return (
      point.land() ||
      point.island() ||
      point.mountain() ||
      point.hotSpotAge > 0 ||
      point.barrierIsland()
    );
  }
}
