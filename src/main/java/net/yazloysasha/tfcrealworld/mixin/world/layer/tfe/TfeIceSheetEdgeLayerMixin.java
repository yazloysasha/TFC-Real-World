package net.yazloysasha.tfcrealworld.mixin.world.layer.tfe;

import static com.newterraearth.tfe.world.NTELayerIds.ICE_SHEET_EDGE;
import static com.newterraearth.tfe.world.NTELayerIds.ICE_SHEET_TUYAS;
import static com.newterraearth.tfe.world.NTELayerIds.ICE_SHEET_TUYAS_EDGE;

import com.newterraearth.tfe.world.layer.NTEIceSheetEdgeLayer;
import net.dries007.tfc.world.layer.framework.AreaContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TFE already rims glaciated volcanic coasts. Map ice sheets still put
 * {@code ICE_SHEET_EDGE} between tuyas, so paint {@code ICE_SHEET_TUYAS_EDGE}
 * on that contact like 1.21.1.
 */
@Mixin(value = NTEIceSheetEdgeLayer.class, remap = false)
public class TfeIceSheetEdgeLayerMixin {

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
