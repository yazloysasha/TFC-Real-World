package net.yazloysasha.tfcrealworld.mixin.world.layer;

import static net.dries007.tfc.world.layer.TFCLayers.GLACIALLY_CARVED_VOLCANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.GLACIATED_VOLCANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS;

import net.dries007.tfc.world.layer.TFCLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TFCLayers.class, remap = false)
public class TFCLayersMixin {

  /**
   * Vanilla shore-converts {@code GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS} into
   * {@code GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS}. Map volcanic coasts are
   * one cell thick, so that wipe removes the glaciated biome entirely.
   */
  @Inject(method = "hasShore", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$keepGlaciatedVolcanicOceanicMountains(
    int value,
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (value == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS) {
      cir.setReturnValue(false);
    }
  }

  /**
   * Vanilla already withholds lakes from non-volcanic glaciated mountains.
   * The volcanic variants were left out, so rare cells become generic
   * {@code LAKE} and vanish from coverage.
   */
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
