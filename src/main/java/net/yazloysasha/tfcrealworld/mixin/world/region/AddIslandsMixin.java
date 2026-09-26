package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddIslands;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Procedural island chains do not belong on a real-world continent map.
 */
@Mixin(value = AddIslands.class, remap = false)
public class AddIslandsMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$skipFakeIslandsWhenUsingContinentMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      ci.cancel();
    }
  }
}
