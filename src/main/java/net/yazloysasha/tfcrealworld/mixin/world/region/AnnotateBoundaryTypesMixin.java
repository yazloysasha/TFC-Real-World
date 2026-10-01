package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateBoundaryTypes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Divergence is a property of the boundary zone a point lies in: negative in
 * convergent zones, positive in divergent zones, zero elsewhere. Vanilla then
 * places rifts, ridges and collisional belts only inside painted zones.
 */
@Mixin(value = AnnotateBoundaryTypes.class, remap = false)
public class AnnotateBoundaryTypesMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$divergenceFromTectonics(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    final TectonicsMap map = TectonicsRegistry.get(context.generator());
    if (map == null) {
      return;
    }
    for (final Region.Point point : context.region.points()) {
      point.divergence = map
        .classAtGrid(point.x, point.z)
        .boundary()
        .divergence();
    }
    ci.cancel();
  }
}
