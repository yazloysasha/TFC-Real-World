package net.yazloysasha.tfcrealworld.mixin.world.layer;

import java.util.function.IntPredicate;
import java.util.function.Predicate;
import net.dries007.tfc.world.layer.MoreShoresLayer;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.AreaContext;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MoreShoresLayer.class, remap = false)
public class MoreShoresLayerMixin {

  private static final int TIDAL_FLATS_CHANCE = 3;

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$shrinkTidalFlats(
    AreaContext context,
    int north,
    int east,
    int south,
    int west,
    int center,
    CallbackInfoReturnable<Integer> cir
  ) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) return;

    Predicate<IntPredicate> any = p ->
      p.test(north) || p.test(east) || p.test(south) || p.test(west);

    if (center == TFCLayers.OCEAN) {
      cir.setReturnValue(center);
      return;
    }

    if (center != TFCLayers.SHORE) {
      cir.setReturnValue(center);
      return;
    }

    if (!any.test(TFCLayers::isOcean) || any.test(TFCLayers::isMountains)) {
      cir.setReturnValue(center);
      return;
    }

    if (
      TIDAL_FLATS_CHANCE > 1 &&
      context.random().nextInt(TIDAL_FLATS_CHANCE) != 0
    ) {
      cir.setReturnValue(center);
      return;
    }

    cir.setReturnValue(TFCLayers.TIDAL_FLATS);
  }
}
