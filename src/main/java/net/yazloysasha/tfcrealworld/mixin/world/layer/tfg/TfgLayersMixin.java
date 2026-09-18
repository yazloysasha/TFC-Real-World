package net.yazloysasha.tfcrealworld.mixin.world.layer.tfg;

import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.ICE_SHEET_TUYAS;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.ICE_SHEET_TUYAS_EDGE;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.TOWER_KARST_BAY;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.terrafirmagreg.core.world.new_ow_wg.TFGLayers;

/**
 * Shore runs before {@code TFGIceSheetEdgeLayer}. Vanilla {@code shoreFor}
 * does not know {@code ICE_SHEET_TUYAS_EDGE}, so a coast rim painted at
 * region scale would become tidal flats. Same idea as 1.21.1
 * {@code TFCLayersMixin} withholding shore from thin glacial coasts.
 */
@Mixin(value = TFGLayers.class, remap = false)
public class TfgLayersMixin {

  @Inject(method = "hasShore", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$keepRareCoastalRims(
    int value,
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (
      value == ICE_SHEET_TUYAS ||
      value == ICE_SHEET_TUYAS_EDGE ||
      value == TOWER_KARST_BAY
    ) {
      cir.setReturnValue(false);
    }
  }
}
