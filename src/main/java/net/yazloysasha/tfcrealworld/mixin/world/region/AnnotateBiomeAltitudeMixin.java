package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateBiomeAltitude;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.DivergenceNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGDivergenceNoise;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overrides biome altitude annotation logic when using altitude map.
 * Instead of BFS from mountains, directly calculates biomeAltitude based on baseLandHeight from map.
 */
@Mixin(value = AnnotateBiomeAltitude.class, remap = false)
public class AnnotateBiomeAltitudeMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideBiomeAltitude(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      calculateBiomeAltitudeFromMap(context.region);
      tfcrealworld$applyMapMountainFlags(context.region, context.generator());
      ci.cancel();
    }
  }

  /**
   * Coastal / volcanic flags for map mountains (procedural placeRange is skipped elsewhere).
   */
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
      if (
        MapTectonics.isNearTrench(divergenceNoise, point.x, point.z) ||
        divergence < MapTectonics.TRENCH_DIVERGENCE
      ) {
        point.setVolcanic();
      }
    }
  }

  /**
   * Calculates biomeAltitude directly based on baseLandHeight from altitude map
   */
  private void calculateBiomeAltitudeFromMap(Region region) {
    final int WIDTH = AnnotateBiomeAltitude.WIDTH;

    for (final var point : region.points()) {
      if (point != null && point.land()) {
        final int baseLandHeight = Byte.toUnsignedInt(point.baseLandHeight);

        if (baseLandHeight >= 16) {
          point.setMountain();
          point.biomeAltitude = (byte) (3 * WIDTH);
        } else if (baseLandHeight >= 8) {
          point.biomeAltitude = (byte) (2 * WIDTH);
        } else if (baseLandHeight >= 3) {
          point.biomeAltitude = (byte) WIDTH;
        } else {
          point.biomeAltitude = 0;
        }
      }
    }
  }
}
