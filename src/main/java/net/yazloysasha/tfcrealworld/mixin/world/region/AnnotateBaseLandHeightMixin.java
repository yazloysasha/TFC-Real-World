package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateBaseLandHeight;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.region.calculator.AltitudeCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnnotateBaseLandHeight.class, remap = false)
public class AnnotateBaseLandHeightMixin {

  /**
   * Keep vanilla {@code distanceToLand} BFS (atolls need it), then overwrite
   * land height / ocean depth from the altitude map.
   */
  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$overrideBaseLandHeight(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }
    new AltitudeCalculator().calculate(context.region, context.generator());
  }
}
