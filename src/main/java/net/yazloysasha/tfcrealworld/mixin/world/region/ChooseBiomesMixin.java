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
  private static final int[] SHALLOW_OCEAN_BIOMES = { OCEAN, OCEAN_REEF };

  @Unique
  private static final int[] LAND_RIFT_SPAWN_ROLL = { 1, 0 };

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
        final int rawDepth = Byte.toUnsignedInt(point.oceanDepth);
        if (rawDepth >= 10) {
          point.biome = DEEP_OCEAN_TRENCH;
        } else {
          final int depth = Byte.toUnsignedInt(
            PNGAltitudeNoise.bucketFromRawOceanDepth(rawDepth)
          );
          final int areaSeed = blobArea.get(point.x, point.z);

          if (depth <= 1) {
            point.biome = OCEAN_REEF;
          } else if (depth == 2) {
            point.biome = point.temperature > 12 && point.distanceToLand > 4
              ? OCEAN_ATOLLS
              : OCEAN;
          } else if (depth == PNGAltitudeNoise.ABYSSAL_OCEAN_DEPTH) {
            point.biome = DEEP_OCEAN;
          } else if (depth >= 4) {
            point.biome = point.temperature > 12 && point.distanceToLand > 3
              ? DEEP_OCEAN_ATOLLS
              : DEEP_OCEAN;
          } else {
            point.biome = accessor.tfcrealworld$invokeRandomSeededFrom(
              rngSeed,
              areaSeed,
              SHALLOW_OCEAN_BIOMES
            );
          }

          if (
            divergenceNoise != null &&
            MapTectonics.isNearOceanRidge(divergenceNoise, point.x, point.z)
          ) {
            point.biome = OCEAN_RIDGE;
          } else if (
            divergenceNoise != null &&
            (point.barrierIsland() ||
              MapTectonics.isNearTrench(divergenceNoise, point.x, point.z)) &&
            depth <= 2
          ) {
            point.biome = OCEANIC_VOLCANIC_ARC;
          }
        }
      }
    }
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
