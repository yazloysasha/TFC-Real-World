package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddMountains;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AddMountains.class, remap = false)
public class AddMountainsMixin {

  @Inject(
    method = "apply(Lnet/dries007/tfc/world/region/RegionGenerator$Context;)V",
    at = @At("HEAD"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$disableAddMountains(CallbackInfo ci) {
    if (TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      ci.cancel();
    }
  }
}
