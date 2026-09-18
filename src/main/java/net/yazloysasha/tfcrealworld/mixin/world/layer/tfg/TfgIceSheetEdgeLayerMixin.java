package net.yazloysasha.tfcrealworld.mixin.world.layer.tfg;

import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.ICE_SHEET_EDGE;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.ICE_SHEET_TUYAS;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.ICE_SHEET_TUYAS_EDGE;

import net.dries007.tfc.world.layer.framework.AreaContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.terrafirmagreg.core.world.new_ow_wg.layers.TFGIceSheetEdgeLayer;

/**
 * Vanilla paints {@code ICE_SHEET_TUYAS_EDGE} on moraine next to tuyas.
 * Map ice sheets put {@code ICE_SHEET_EDGE} in that contact instead.
 * Those two share the ice-edge climate (glacial base + edge surface); the
 * tuya-edge id only adds tuyas. Do not convert shores or warmer land.
 */
@Mixin(value = TFGIceSheetEdgeLayer.class, remap = false)
public class TfgIceSheetEdgeLayerMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$tuyasEdgeOnIceSheetRim(
    AreaContext context,
    int north,
    int east,
    int south,
    int west,
    int center,
    CallbackInfoReturnable<Integer> cir
  ) {
    if (
      center == ICE_SHEET_EDGE &&
      (north == ICE_SHEET_TUYAS ||
        east == ICE_SHEET_TUYAS ||
        south == ICE_SHEET_TUYAS ||
        west == ICE_SHEET_TUYAS)
    ) {
      cir.setReturnValue(ICE_SHEET_TUYAS_EDGE);
    }
  }
}
