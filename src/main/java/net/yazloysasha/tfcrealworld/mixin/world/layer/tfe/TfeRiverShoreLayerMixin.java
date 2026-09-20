package net.yazloysasha.tfcrealworld.mixin.world.layer.tfe;

import static com.newterraearth.tfe.world.NTELayerIds.GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS;

import com.newterraearth.tfe.world.layer.NTERiverShoreLayer;
import net.dries007.tfc.world.layer.framework.AreaContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TFE's shore pass uses a private {@code hasShore} copy, so
 * {@link TfeTFCLayersMixin} never sees {@code NTERiverShoreLayer}. Keep the
 * one-cell map glaciated volcanic coast like 1.21.1 {@code TFCLayersMixin}.
 */
@Mixin(value = NTERiverShoreLayer.class, remap = false)
public class TfeRiverShoreLayerMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$keepGlaciatedVolcanicOceanicMountains(
    AreaContext context,
    int north,
    int east,
    int south,
    int west,
    int center,
    CallbackInfoReturnable<Integer> cir
  ) {
    if (center == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS) {
      cir.setReturnValue(center);
    }
  }
}
