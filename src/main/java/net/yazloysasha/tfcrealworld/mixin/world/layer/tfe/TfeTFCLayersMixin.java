package net.yazloysasha.tfcrealworld.mixin.world.layer.tfe;

import static com.newterraearth.tfe.world.NTELayerIds.GLACIALLY_CARVED_VOLCANIC_MOUNTAINS;
import static com.newterraearth.tfe.world.NTELayerIds.GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS;
import static com.newterraearth.tfe.world.NTELayerIds.GLACIATED_VOLCANIC_MOUNTAINS;
import static com.newterraearth.tfe.world.NTELayerIds.GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS;

import net.dries007.tfc.world.layer.TFCLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TFE's {@code TFCLayers.hasShore} still shore-converts one-cell map
 * {@code GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS}. The live shore pass is
 * {@link TfeRiverShoreLayerMixin}; this keeps the public helper in sync.
 */
@Mixin(value = TFCLayers.class, remap = false, priority = 1500)
public class TfeTFCLayersMixin {

  @Inject(method = "hasShore", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$keepGlaciatedVolcanicOceanicMountains(
    int value,
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (value == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS) {
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "hasLake", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$noLakesOnVolcanicGlacialMountains(
    int value,
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (
      value == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
      value == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS ||
      value == GLACIATED_VOLCANIC_MOUNTAINS ||
      value == GLACIALLY_CARVED_VOLCANIC_MOUNTAINS
    ) {
      cir.setReturnValue(false);
    }
  }
}
