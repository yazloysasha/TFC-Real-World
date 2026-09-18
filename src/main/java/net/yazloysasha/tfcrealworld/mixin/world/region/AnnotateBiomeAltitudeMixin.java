package net.yazloysasha.tfcrealworld.mixin.world.region;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.util.BitSet;
import net.dries007.tfc.world.region.AnnotateBiomeAltitude;
import net.dries007.tfc.world.region.Region;
import net.minecraft.world.level.levelgen.RandomSource;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.RegionContextHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnnotateBiomeAltitude.class, remap = false)
public class AnnotateBiomeAltitudeMixin {

  @Unique
  private static final short FLAG_MOUNTAIN = 0b10000;

  @Unique
  private static final int MAP_MID_LAND_HEIGHT = 5;

  @Unique
  private static final int MAP_HIGH_LAND_HEIGHT = 11;

  @Unique
  private static final int MAP_MOUNTAIN_CAP_LAND_HEIGHT = 16;

  @Inject(
    method = "apply(Lnet/dries007/tfc/world/region/RegionGenerator$Context;)V",
    at = @At("HEAD"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideBiomeAltitude(CallbackInfo ci) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }

    final Region region = RegionContextHolder.getRegion();
    final RandomSource random = RegionContextHolder.getRandom();
    if (region == null || random == null) {
      return;
    }

    tfcrealworld$annotateFromMap(region, random);
    tfcrealworld$applyCoastalMountainFlags(region);
    ci.cancel();
  }

  @Unique
  private static void tfcrealworld$applyCoastalMountainFlags(Region region) {
    for (final Region.Point point : region.data()) {
      if (point == null || !point.land() || !point.mountain()) {
        continue;
      }
      if (point.distanceToOcean < 3) {
        point.setCoastalMountain();
      }
    }
  }

  @Unique
  private static void tfcrealworld$annotateFromMap(
    Region region,
    RandomSource random
  ) {
    final int width = AnnotateBiomeAltitude.WIDTH;
    final Region.Point[] data = region.data();

    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null || !point.land()) {
        continue;
      }
      final int baseLandHeight = Byte.toUnsignedInt(point.baseLandHeight);
      if (tfcrealworld$isMapMountainCore(region, index, baseLandHeight)) {
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

  @Unique
  private static void tfcrealworld$capBiomeAltitudeByMapHeight(
    Region region,
    int width
  ) {
    for (final Region.Point point : region.data()) {
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
    int index,
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
          final int neighborIndex = region.offset(index, dx, dz);
          if (neighborIndex == -1) {
            continue;
          }
          final Region.Point neighbor = region.data()[neighborIndex];
          if (neighbor == null || !neighbor.land()) {
            continue;
          }
          final int height = Byte.toUnsignedInt(neighbor.baseLandHeight);
          if (height >= MAP_MOUNTAIN_CAP_LAND_HEIGHT) {
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
      if (baseLandHeight < MAP_MOUNTAIN_CAP_LAND_HEIGHT) {
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
    final Region.Point[] data = region.data();
    final BitSet explored = new BitSet(data.length);
    final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();

    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point != null && point.land() && point.mountain()) {
        point.biomeAltitude = (byte) (3 * width);
        queue.enqueue(index);
        explored.set(index);
      }
    }

    while (!queue.isEmpty()) {
      final int last = queue.dequeueInt();
      final Region.Point lastPoint = data[last];
      final int nextAltitude = lastPoint.biomeAltitude - 1;
      if (nextAltitude < 0) {
        continue;
      }

      for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
          final int next = region.offset(last, dx, dz);
          if (next == -1) {
            continue;
          }
          final Region.Point point = data[next];
          if (
            point != null &&
            point.land() &&
            point.biomeAltitude == 0 &&
            !explored.get(next)
          ) {
            if (
              random.nextInt(13) == 0 && lastPoint.biomeAltitude != 3 * width
            ) {
              point.biomeAltitude = lastPoint.biomeAltitude;
              queue.enqueueFirst(next);
            } else {
              point.biomeAltitude = (byte) nextAltitude;
              queue.enqueue(next);
            }
            explored.set(next);
          }
        }
      }
    }
  }

  @Unique
  private static void tfcrealworld$raiseByLandHeight(Region region, int width) {
    for (final Region.Point point : region.data()) {
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
