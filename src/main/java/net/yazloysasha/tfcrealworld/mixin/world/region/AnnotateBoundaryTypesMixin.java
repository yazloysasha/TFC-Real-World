package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateBoundaryTypes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When using a continent map, procedural plate-boundary divergence (Voronoi cell
 * edges) must not run — it conflicts with real-world coastlines and creates fake rifts.
 */
@Mixin(value = AnnotateBoundaryTypes.class, remap = false)
public class AnnotateBoundaryTypesMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$neutralizeDivergenceFromMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return;
    }

    for (final Region.Point point : context.region.points()) {
      point.divergence = 0f;
    }
    ci.cancel();
  }
}
