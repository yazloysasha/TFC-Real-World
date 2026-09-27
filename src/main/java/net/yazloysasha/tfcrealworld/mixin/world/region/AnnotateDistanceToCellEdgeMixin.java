package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateDistanceToCellEdge;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skip vanilla Voronoi cell-edge BFS when map tectonics are active. Edges are
 * filled from {@code divergence.png} in {@link AnnotateBoundaryTypesMixin}.
 */
@Mixin(value = AnnotateDistanceToCellEdge.class, remap = false)
public class AnnotateDistanceToCellEdgeMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$skipCellEdgesWhenMapTectonics(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!MapTectonics.isActive(context.generator())) {
      return;
    }
    for (final Region.Point point : context.region.points()) {
      point.distanceToEdge = MapTectonics.INTERIOR_EDGE_DISTANCE;
    }
    ci.cancel();
  }
}
