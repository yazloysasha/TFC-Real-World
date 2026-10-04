package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddContinents;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.region.MapRegionTasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TFC 3 makes the points of a region here, so the task runs and the map
 * then decides which of them are land. TerraFirmaEarth rewrites the task and
 * also measures plate boundaries in it, which the same pass replaces.
 */
@Mixin(value = AddContinents.class, remap = false, priority = 1500)
public class AddContinentsMixin {

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$continentsFromMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    MapRegionTasks.continents(context);
  }
}
