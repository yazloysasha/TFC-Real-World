package net.yazloysasha.tfcrealworld.mixin.world.layer;

import net.dries007.tfc.world.layer.MoreShoresLayer;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.AreaContext;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * It is necessary to prevent the straits from being blocked by land
 */
@Mixin(value = MoreShoresLayer.class, remap = false)
public class MoreShoresLayerMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$keepOceanInNarrowStraits(
    AreaContext context,
    int north,
    int east,
    int south,
    int west,
    int center,
    CallbackInfoReturnable<Integer> cir
  ) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return;
    }
    if (TFCLayers.isOcean(center)) {
      cir.setReturnValue(center);
    }
  }
}
