package net.yazloysasha.tfcrealworld.mixin.world.region.tfg;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.terrafirmagreg.core.world.new_ow_wg.region.TFGAddIslands;

/** The islands of the continent map are the islands: no procedural ones. */
@Mixin(value = TFGAddIslands.class, remap = false)
public class TfgAddIslandsMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$skipProceduralIslandsWhenUsingContinentMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      ci.cancel();
    }
  }
}
