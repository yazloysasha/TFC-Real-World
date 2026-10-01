package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateDistanceToCellEdge;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replace Voronoi cell edges with the distance to the nearest plate-boundary
 * zone painted in {@code tectonics.png}.
 */
@Mixin(value = AnnotateDistanceToCellEdge.class, remap = false)
public class AnnotateDistanceToCellEdgeMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$distanceToPlateBoundary(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    final TectonicsMap map = TectonicsRegistry.get(context.generator());
    if (map == null) {
      return;
    }
    for (final Region.Point point : context.region.points()) {
      point.distanceToEdge = map.distanceToBoundaryAtGrid(point.x, point.z);
    }
    ci.cancel();
  }
}
