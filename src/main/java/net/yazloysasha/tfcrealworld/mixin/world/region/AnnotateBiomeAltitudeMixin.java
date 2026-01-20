package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateBiomeAltitude;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.RegionContextHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnnotateBiomeAltitude.class, remap = false)
public class AnnotateBiomeAltitudeMixin {

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
    if (region == null) {
      return;
    }

    calculateBiomeAltitudeFromMap(region);
    ci.cancel();
  }

  private void calculateBiomeAltitudeFromMap(Region region) {
    final int WIDTH = AnnotateBiomeAltitude.WIDTH;

    for (final var point : region.data()) {
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
