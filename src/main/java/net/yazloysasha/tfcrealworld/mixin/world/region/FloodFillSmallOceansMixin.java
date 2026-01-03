package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.FloodFillSmallOceans;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to override logic for removing small oceans.
 * When using continent map, flood fill stage is skipped
 * to preserve inland seas drawn on the map.
 */
@Mixin(FloodFillSmallOceans.class)
public class FloodFillSmallOceansMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$skipFloodFillWhenUsingContinentMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      ci.cancel();
    }
  }
}
