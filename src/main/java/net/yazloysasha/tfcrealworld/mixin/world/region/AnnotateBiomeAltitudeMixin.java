package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateBiomeAltitude;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overrides biome altitude annotation logic when using altitude map.
 * Instead of BFS from mountains, directly calculates biomeAltitude based on baseLandHeight from map.
 */
@Mixin(AnnotateBiomeAltitude.class)
public class AnnotateBiomeAltitudeMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideBiomeAltitude(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      calculateBiomeAltitudeFromMap(context.region);
      ci.cancel();
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
