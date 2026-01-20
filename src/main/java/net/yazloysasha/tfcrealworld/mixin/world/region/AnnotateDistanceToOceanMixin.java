package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateDistanceToOcean;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.RegionContextHolder;
import net.yazloysasha.tfcrealworld.world.region.calculator.OceanDistanceCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnnotateDistanceToOcean.class, remap = false)
public class AnnotateDistanceToOceanMixin {

  @Inject(
    method = "apply(Lnet/dries007/tfc/world/region/RegionGenerator$Context;)V",
    at = @At("HEAD"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideDistanceToOcean(CallbackInfo ci) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return;
    }

    final var region = RegionContextHolder.getRegion();
    final var generator = RegionContextHolder.getGenerator();
    if (region == null || generator == null) {
      return;
    }

    new OceanDistanceCalculator().calculate(region, generator);
    ci.cancel();
  }
}
