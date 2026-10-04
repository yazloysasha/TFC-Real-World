package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateDistanceToCellEdge;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.region.MapRegionTasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Plate boundaries of the tectonics map in place of Voronoi cell edges. */
@Mixin(value = AnnotateDistanceToCellEdge.class, remap = false)
public class AnnotateDistanceToCellEdgeMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$fromMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (MapRegionTasks.distanceToCellEdge(context)) {
      ci.cancel();
    }
  }
}
