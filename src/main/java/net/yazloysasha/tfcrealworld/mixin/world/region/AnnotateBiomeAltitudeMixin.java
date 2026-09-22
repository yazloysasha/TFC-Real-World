package net.yazloysasha.tfcrealworld.mixin.world.region;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.util.BitSet;
import net.dries007.tfc.world.region.AnnotateBiomeAltitude;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.minecraft.util.RandomSource;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.DivergenceNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGDivergenceNoise;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import net.yazloysasha.tfcrealworld.world.region.calculator.AltitudeCalculator;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnnotateBiomeAltitude.class, remap = false, priority = 500)
public class AnnotateBiomeAltitudeMixin {

  @Unique
  private static final short FLAG_MOUNTAIN = 0b10000;

  @Unique
  private static final int MAP_MID_LAND_HEIGHT = 5;

  @Unique
  private static final int MAP_HIGH_LAND_HEIGHT = 11;

  @Unique
  private static final int MAP_MOUNTAIN_CAP_LAND_HEIGHT = 16;

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideBiomeAltitude(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      new AltitudeCalculator().calculate(context.region, context.generator());
      tfcrealworld$annotateFromMap(context.region, context.random);
      tfcrealworld$applyMapMountainFlags(context.region, context.generator());
      ci.cancel();
    }
  }

  @Unique
  private static void tfcrealworld$applyMapMountainFlags(
    Region region,
    RegionGenerator generator
  ) {
    final PNGDivergenceNoise divergenceNoise = DivergenceNoiseRegistry.get(
      generator
    );
    if (!MapTectonics.isActive(generator) || divergenceNoise == null) {
      return;
    }

    for (final Region.Point point : region.points()) {
      if (point == null || !point.land() || !point.mountain()) {
        continue;
      }
      final float divergence = divergenceNoise.getDivergence(point.x, point.z);
      point.divergence = divergence;
      if (point.distanceToOcean < 3) {
        point.setCoastalMountain();
      }
      if (point.divergence < MapTectonics.TRENCH_DIVERGENCE) {
        point.setVolcanic();
      }
    }
  }

  @Unique
  private static void tfcrealworld$annotateFromMap(
    Region region,
    RandomSource random
  ) {
    final int width = AnnotateBiomeAltitude.WIDTH;

    for (final var point : region.points()) {
      if (point == null || !point.land()) {
        continue;
      }
      final int baseLandHeight = Byte.toUnsignedInt(point.baseLandHeight);
      if (tfcrealworld$isMapMountainCore(region, point, baseLandHeight)) {
        point.setMountain();
        point.biomeAltitude = (byte) (3 * width);
      } else {
        tfcrealworld$clearMountain(point);
        point.biomeAltitude = 0;
      }
    }

    tfcrealworld$bfsFromMountains(region, random, width);
    tfcrealworld$raiseByLandHeight(region, width);
    tfcrealworld$capBiomeAltitudeByMapHeight(region, width);
  }

  /**
   * Clamp vanilla mountain BFS to the discrete band allowed by PNG height.
   */
  @Unique
  private static void tfcrealworld$capBiomeAltitudeByMapHeight(
    Region region,
    int width
  ) {
    for (final var point : region.points()) {
      if (point == null || !point.land() || point.mountain()) {
        continue;
      }
      final int maxDisc = tfcrealworld$maxDiscreteAltitudeForLandHeight(
        Byte.toUnsignedInt(point.baseLandHeight)
      );
      if (point.discreteBiomeAltitude() > maxDisc) {
        point.biomeAltitude = (byte) (maxDisc * width);
      }
    }
  }

  @Unique
  private static int tfcrealworld$maxDiscreteAltitudeForLandHeight(
    int baseLandHeight
  ) {
    if (baseLandHeight >= MAP_MOUNTAIN_CAP_LAND_HEIGHT) {
      return 3;
    }
    if (baseLandHeight >= MAP_HIGH_LAND_HEIGHT) {
      return 2;
    }
    if (baseLandHeight >= MAP_MID_LAND_HEIGHT) {
      return 1;
    }
    return 0;
  }

  @Unique
  private static boolean tfcrealworld$isMapMountainCore(
    Region region,
    Region.Point point,
    int baseLandHeight
  ) {
    if (baseLandHeight >= 18) {
      return true;
    }
    if (baseLandHeight >= 15) {
      int countAtLeast15 = 0;
      boolean anyAtLeast16 = false;
      boolean anyAtLeast14 = false;
      for (int dz = -1; dz <= 1; dz++) {
        for (int dx = -1; dx <= 1; dx++) {
          if (dx == 0 && dz == 0) {
            continue;
          }
          final Region.Point neighbor = region.atOffset(point.index, dx, dz);
          if (neighbor == null || !neighbor.land()) {
            continue;
          }
          final int height = Byte.toUnsignedInt(neighbor.baseLandHeight);
          if (height >= 16) {
            anyAtLeast16 = true;
          }
          if (height >= 15) {
            countAtLeast15++;
          }
          if (height >= 14) {
            anyAtLeast14 = true;
          }
        }
      }
      if (anyAtLeast16 || countAtLeast15 >= 2) {
        return true;
      }
      if (baseLandHeight < 16) {
        return false;
      }
      return anyAtLeast14;
    }
    return false;
  }

  @Unique
  private static void tfcrealworld$clearMountain(Region.Point point) {
    if (!point.mountain()) {
      return;
    }
    final RegionPointAccessor flags = (RegionPointAccessor) (Object) point;
    flags.tfcrealworld$setFlags(
      (short) (flags.tfcrealworld$getFlags() & ~FLAG_MOUNTAIN)
    );
  }

  @Unique
  private static void tfcrealworld$bfsFromMountains(
    Region region,
    RandomSource random,
    int width
  ) {
    final BitSet explored = new BitSet(region.size());
    final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();

    for (final var point : region.points()) {
      if (point != null && point.land() && point.mountain()) {
        point.biomeAltitude = (byte) (3 * width);
        queue.enqueue(point.index);
        explored.set(point.index);
      }
    }

    while (!queue.isEmpty()) {
      final int last = queue.dequeueInt();
      final Region.Point lastPoint = region.atIndex(last);
      final int nextAltitude = lastPoint.biomeAltitude - 1;
      if (nextAltitude < 0) {
        continue;
      }

      for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
          @Nullable
          final Region.Point point = region.atOffset(last, dx, dz);
          if (
            point != null &&
            point.land() &&
            point.biomeAltitude == 0 &&
            !explored.get(point.index)
          ) {
            if (
              random.nextInt(13) == 0 && lastPoint.biomeAltitude != 3 * width
            ) {
              point.biomeAltitude = lastPoint.biomeAltitude;
              queue.enqueueFirst(point.index);
            } else {
              point.biomeAltitude = (byte) nextAltitude;
              queue.enqueue(point.index);
            }
            explored.set(point.index);
          }
        }
      }
    }
  }

  @Unique
  private static void tfcrealworld$raiseByLandHeight(Region region, int width) {
    for (final var point : region.points()) {
      if (
        point == null ||
        !point.land() ||
        point.mountain() ||
        point.discreteBiomeAltitude() != 0
      ) {
        continue;
      }
      final int baseLandHeight = Byte.toUnsignedInt(point.baseLandHeight);
      if (baseLandHeight >= MAP_MID_LAND_HEIGHT) {
        point.biomeAltitude = (byte) width;
      }
      if (
        point.discreteBiomeAltitude() == 1 &&
        baseLandHeight >= MAP_HIGH_LAND_HEIGHT
      ) {
        point.biomeAltitude = (byte) (2 * width);
      }
    }
  }
}
