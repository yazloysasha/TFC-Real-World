package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateBaseLandHeight;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnnotateBaseLandHeight.class, remap = false)
public class AnnotateBaseLandHeightMixin {

  /**
   * Keep vanilla {@code distanceToLand} BFS (atolls need it), then overwrite
   * land height from the tectonic land relief.
   */
  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$baseLandHeightFromRelief(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    final TectonicsMap map = TectonicsRegistry.get(context.generator());
    if (map == null) {
      return;
    }
    for (final var point : context.region.points()) {
      if (point != null && point.land()) {
        point.baseLandHeight = map
          .classAtGrid(point.x, point.z)
          .land()
          .baseLandHeight();
      }
    }
  }
}
