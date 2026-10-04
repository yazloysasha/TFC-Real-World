package net.yazloysasha.tfcrealworld.mixin.world.region.tfg;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.region.MapRegionTasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.terrafirmagreg.core.world.new_ow_wg.region.TFGAddContinents;

/** Land and water of the continent map over the points of the region. */
@Mixin(value = TFGAddContinents.class, remap = false)
public class TfgAddContinentsMixin {

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$fromMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    MapRegionTasks.continents(context);
  }
}
