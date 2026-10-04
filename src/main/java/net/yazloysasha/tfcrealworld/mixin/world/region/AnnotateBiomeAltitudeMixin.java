package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateBiomeAltitude;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.region.MapRegionTasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Biome altitude and mountains from the relief of the tectonics map. */
@Mixin(value = AnnotateBiomeAltitude.class, remap = false, priority = 500)
public class AnnotateBiomeAltitudeMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$fromMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (MapRegionTasks.biomeAltitude(context)) {
      ci.cancel();
    }
  }
}
