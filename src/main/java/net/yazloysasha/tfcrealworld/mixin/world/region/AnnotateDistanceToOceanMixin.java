package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateDistanceToOcean;
import net.yazloysasha.tfcrealworld.world.region.MapRegionTasks;
import net.yazloysasha.tfcrealworld.world.region.RegionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Distance to the ocean of the continent map. */
@Mixin(value = AnnotateDistanceToOcean.class, remap = false)
public class AnnotateDistanceToOceanMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$fromMap(CallbackInfo ci) {
    final RegionContext context = RegionContext.current();
    if (MapRegionTasks.distanceToOcean(context)) {
      ci.cancel();
    }
  }
}
