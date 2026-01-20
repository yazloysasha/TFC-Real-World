package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateBaseLandHeight;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.RegionContextHolder;
import net.yazloysasha.tfcrealworld.world.region.calculator.AltitudeCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnnotateBaseLandHeight.class, remap = false)
public class AnnotateBaseLandHeightMixin {

  @Inject(
    method = "apply(Lnet/dries007/tfc/world/region/RegionGenerator$Context;)V",
    at = @At("HEAD"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideBaseLandHeight(CallbackInfo ci) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }

    final var region = RegionContextHolder.getRegion();
    final var generator = RegionContextHolder.getGenerator();
    if (region == null || generator == null) {
      return;
    }

    new AltitudeCalculator().calculate(region, generator);
    ci.cancel();
  }
}
