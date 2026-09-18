package net.yazloysasha.tfcrealworld.mixin.world.layer.tfg;

import java.util.function.IntPredicate;
import java.util.function.Predicate;
import net.dries007.tfc.world.layer.framework.AreaContext;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.terrafirmagreg.core.world.new_ow_wg.TFGLayers;
import su.terrafirmagreg.core.world.new_ow_wg.layers.TFGShoreLayer;

/**
 * TFG {@code shoreFor} defaults many coasts to tidal flats. Same 1-in-3 keep
 * as 1.21.1 {@code MoreShoresLayerMixin}.
 */
@Mixin(value = TFGShoreLayer.class, remap = false)
public class TfgShoreLayerMixin {

  @Unique
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
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return;
    }
    if (TFGLayers.isOcean(center) || !TFGLayers.hasShore(center)) {
      return;
    }
    final Predicate<IntPredicate> any = p ->
      p.test(north) || p.test(east) || p.test(south) || p.test(west);
    if (!any.test(TFGLayers::isOcean)) {
      return;
    }

    final int shore = TFGLayers.shoreFor(center);
    if (shore != TFGLayers.TIDAL_FLATS) {
      cir.setReturnValue(shore);
      return;
    }
    if (
      TIDAL_FLATS_CHANCE > 1 &&
      context.random().nextInt(TIDAL_FLATS_CHANCE) != 0
    ) {
      cir.setReturnValue(TFGLayers.SHORE);
      return;
    }
    cir.setReturnValue(TFGLayers.TIDAL_FLATS);
  }
}
