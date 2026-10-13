package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddContinents;
import net.yazloysasha.tfcrealworld.world.region.MapRegionTasks;
import net.yazloysasha.tfcrealworld.world.region.RegionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TFC makes the points of a region here, so the task runs and the map
 * then decides which of them are land.
 */
@Mixin(value = AddContinents.class, remap = false, priority = 1500)
public class AddContinentsMixin {

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$continentsFromMap(CallbackInfo ci) {
    final RegionContext context = RegionContext.current();
    MapRegionTasks.continents(context);
  }
}
