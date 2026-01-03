package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddHotspots;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.PNGHotspotsNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AddHotspots.class)
public class AddHotspotsMixin {

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$applyHotspotAgeFromMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      final PNGHotspotsNoise hotspotsNoise = HotspotsNoiseRegistry.get(
        context.generator()
      );
      if (hotspotsNoise == null) {
        return;
      }

      for (final var point : context.region.points()) {
        if (point != null) {
          byte mapAge = hotspotsNoise.getHotSpotAge(
            (double) point.x,
            (double) point.z
          );
          if (mapAge > 0) {
            point.hotSpotAge = mapAge;
          }
        }
      }
    }
  }
}
