package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateDistanceToOcean;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.region.OceanDistanceCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnnotateDistanceToOcean.class)
public class AnnotateDistanceToOceanMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void realworld$overrideDistanceToOcean(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      OceanDistanceCalculator.calculateDistanceToOcean(
        context.region,
        context.generator()
      );
      ci.cancel();
    }
  }
}
