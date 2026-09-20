package net.yazloysasha.tfcrealworld.world.backport;

import java.lang.reflect.Field;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.compat.TfeCompat;
import net.yazloysasha.tfcrealworld.compat.TfgCompat;
import net.yazloysasha.tfcrealworld.compat.tfe.TfeBindings;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.koppen.KoppenBasedRainfallVarianceNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGHotspotsNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;
import sun.misc.Unsafe;

/**
 * Overwrites Unique hotspot (and TFE rainfall-variance) fields that backports
 * add onto {@code RegionGenerator}. Missing or renamed fields are skipped so a
 * TFE/TFG bump does not crash worldgen.
 */
public final class HotspotGeneratorNoises {

  private static final String TFG_INTENSITY = "tfg$hotSpotIntensityNoise";
  private static final String TFG_AGE = "tfg$hotSpotAgeNoise";

  private static final Unsafe UNSAFE;

  static {
    try {
      final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      UNSAFE = (Unsafe) unsafeField.get(null);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get Unsafe instance", e);
    }
  }

  private HotspotGeneratorNoises() {}

  public static void overwriteHotspots(
    Object regionGenerator,
    PNGHotspotsNoise hotspotsNoise
  ) {
    if (TfgCompat.isModPresent()) {
      overwriteField(regionGenerator, TFG_INTENSITY, hotspotsNoise);
      overwriteField(
        regionGenerator,
        TFG_AGE,
        (Noise2D) (x, z) -> hotspotsNoise.getHotSpotAge(x, z)
      );
    }
    if (TfeCompat.isModPresent()) {
      overwriteField(
        regionGenerator,
        TfeBindings.HOTSPOT_INTENSITY_FIELD,
        hotspotsNoise
      );
      overwriteField(
        regionGenerator,
        TfeBindings.HOTSPOT_AGE_FIELD,
        (Noise2D) (x, z) -> hotspotsNoise.getHotSpotAge(x, z)
      );
    }
  }

  /**
   * TFE {@code <init>} and {@code nte$setRootLevelSeed} rebuild Unique
   * hotspot / rain-var noises. Re-stamp the map versions afterwards so
   * {@code AnnotateClimate} and shield heightmaps match 1.21.1.
   */
  public static void reapplyMapNoises(RegionGenerator generator) {
    if (TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      final PNGHotspotsNoise hotspotsNoise = HotspotsNoiseRegistry.get(
        generator
      );
      if (hotspotsNoise != null) {
        overwriteHotspots(generator, hotspotsNoise);
      }
    }
    if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get() && TfeCompat.isModPresent()) {
      final int horizontalScale = TFCRealWorldConfig.HORIZONTAL_SCALE.get();
      final int verticalScale = TFCRealWorldConfig.VERTICAL_SCALE.get();
      overwriteRainfallVariance(
        generator,
        new KoppenBasedRainfallVarianceNoise(
          new PNGKoppenNoise(horizontalScale, verticalScale),
          new PNGTemperatureNoise(horizontalScale, verticalScale),
          new PNGRainfallNoise(horizontalScale, verticalScale)
        )
      );
    }
  }

  public static void overwriteRainfallVariance(
    Object regionGenerator,
    Noise2D rainfallVariance
  ) {
    if (!TfeCompat.isModPresent()) {
      return;
    }
    overwriteField(
      regionGenerator,
      TfeBindings.RAINFALL_VARIANCE_FIELD,
      rainfallVariance
    );
  }

  private static void overwriteField(
    Object instance,
    String name,
    Object value
  ) {
    try {
      final Field field = findDeclaredField(instance.getClass(), name);
      if (field == null) {
        return;
      }
      UNSAFE.putObject(instance, UNSAFE.objectFieldOffset(field), value);
    } catch (Exception ignored) {
      // Later mixins still feed map ages / climate through task hooks.
    }
  }

  private static Field findDeclaredField(Class<?> type, String name) {
    Class<?> cursor = type;
    while (cursor != null && cursor != Object.class) {
      try {
        return cursor.getDeclaredField(name);
      } catch (NoSuchFieldException ignored) {
        cursor = cursor.getSuperclass();
      }
    }
    return null;
  }
}
