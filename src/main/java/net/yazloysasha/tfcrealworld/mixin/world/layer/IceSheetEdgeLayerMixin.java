package net.yazloysasha.tfcrealworld.mixin.world.layer;

import static net.dries007.tfc.world.layer.TFCLayers.GLACIALLY_CARVED_VOLCANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.GLACIATED_VOLCANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.ICE_SHEET_EDGE;
import static net.dries007.tfc.world.layer.TFCLayers.ICE_SHEET_TUYAS;
import static net.dries007.tfc.world.layer.TFCLayers.ICE_SHEET_TUYAS_EDGE;

import net.dries007.tfc.world.layer.IceSheetEdgeLayer;
import net.dries007.tfc.world.layer.framework.AreaContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IceSheetEdgeLayer.class, remap = false)
public class IceSheetEdgeLayerMixin {

  /**
   * Vanilla only paints {@code ICE_SHEET_TUYAS_EDGE} on moraine biomes that
   * touch tuyas. Map ice sheets put {@code ICE_SHEET_EDGE} between them, so
   * that contact never happens. Treat the ice-sheet rim the same way.
   *
   * <p>Vanilla also rims non-volcanic glaciated mountains with glacially
   * carved neighbors, but never the volcanic oceanic pair. Add that rim so
   * {@code GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS} can exist beside a
   * one-cell glaciated coast.
   */
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
      return;
    }

    if (!IceSheetEdgeLayer.isNotIceSheetOrGlaciated(center)) {
      return;
    }
    if (
      tfcrealworld$touches(
        north,
        east,
        south,
        west,
        GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS
      )
    ) {
      cir.setReturnValue(GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS);
      return;
    }
    if (
      tfcrealworld$touches(
        north,
        east,
        south,
        west,
        GLACIATED_VOLCANIC_MOUNTAINS
      )
    ) {
      cir.setReturnValue(GLACIALLY_CARVED_VOLCANIC_MOUNTAINS);
    }
  }

  @Unique
  private static boolean tfcrealworld$touches(
    int north,
    int east,
    int south,
    int west,
    int biome
  ) {
    return north == biome || east == biome || south == biome || west == biome;
  }
}
