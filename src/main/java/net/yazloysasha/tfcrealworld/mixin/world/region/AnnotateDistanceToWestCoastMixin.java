package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateDistanceToWestCoast;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.region.WestCoastDistanceCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnnotateDistanceToWestCoast.class)
public class AnnotateDistanceToWestCoastMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideDistanceToWestCoast(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (TFCRealWorldConfig.getContinentFromMap()) {
      WestCoastDistanceCalculator.calculateDistanceToWestCoast(
        context.region,
        context.generator()
      );
      ci.cancel();
    }
  }
}
