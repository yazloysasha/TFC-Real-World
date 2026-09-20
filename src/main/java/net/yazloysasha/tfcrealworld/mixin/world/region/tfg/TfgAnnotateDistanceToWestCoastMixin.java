package net.yazloysasha.tfcrealworld.mixin.world.region.tfg;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.backport.WestCoastFromMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.terrafirmagreg.core.world.new_ow_wg.region.IRegionPoint;
import su.terrafirmagreg.core.world.new_ow_wg.region.TFGAnnotateDistanceToWestCoast;

@Mixin(value = TFGAnnotateDistanceToWestCoast.class, remap = false)
public class TfgAnnotateDistanceToWestCoastMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideDistanceToWestCoast(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (
      WestCoastFromMap.apply(context.region, (point, dist) ->
        ((IRegionPoint) point).tfg$setDistanceToWestCoast(dist)
      )
    ) {
      ci.cancel();
    }
  }
}
