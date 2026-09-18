package net.yazloysasha.tfcrealworld.mixin.world.layer.tfg;

import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.DRUMLINS;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.HILLS;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.ICE_SHEET_EDGE;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.ICE_SHEET_OCEANIC;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.ICE_SHEET_SHORE;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.ICE_SHEET_TUYAS;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.ICE_SHEET_TUYAS_EDGE;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.INVERTED_PATTERNED_GROUND;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.KNOB_AND_KETTLE;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.LOWLANDS;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.MELTWATER_LAKE;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.PATTERNED_GROUND;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.PLAINS;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.ROLLING_HILLS;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.SHORE;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.STONE_CIRCLES;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.TIDAL_FLATS;
import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.TUYAS;

import net.dries007.tfc.world.layer.framework.AreaContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.terrafirmagreg.core.world.new_ow_wg.layers.TFGIceSheetEdgeLayer;

/**
 * TFG only paints {@code ICE_SHEET_TUYAS_EDGE} on moraine that already touches
 * tuyas. Map Köppen jumps EF/ET to DFC and ShoreLayer turns the climate rim
 * into {@code ICE_SHEET_SHORE} before this layer runs, so that contact never
 * happens. Same 1.21.1 {@code IceSheetEdgeLayerMixin} rule, plus the shore /
 * oceanic / paleo cells the map actually leaves beside tuyas.
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
      tfcrealworld$isTuyasEdgeRim(center) &&
      tfcrealworld$touches(north, east, south, west, ICE_SHEET_TUYAS)
    ) {
      cir.setReturnValue(ICE_SHEET_TUYAS_EDGE);
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

  @Unique
  private static boolean tfcrealworld$isTuyasEdgeRim(int biome) {
    return (
      biome == ICE_SHEET_EDGE ||
      biome == ICE_SHEET_SHORE ||
      biome == ICE_SHEET_OCEANIC ||
      biome == MELTWATER_LAKE ||
      biome == KNOB_AND_KETTLE ||
      biome == PATTERNED_GROUND ||
      biome == INVERTED_PATTERNED_GROUND ||
      biome == STONE_CIRCLES ||
      biome == DRUMLINS ||
      biome == TUYAS ||
      biome == SHORE ||
      biome == TIDAL_FLATS ||
      biome == PLAINS ||
      biome == HILLS ||
      biome == LOWLANDS ||
      biome == ROLLING_HILLS
    );
  }
}
