package net.yazloysasha.tfcrealworld.mixin.world.layer.tfg;

import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.backend.Backends;
import net.yazloysasha.tfcrealworld.world.layer.MapBiomeLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.terrafirmagreg.core.world.new_ow_wg.TFGLayers;

/** Region biome layers that follow the coastline of the continent map. */
@Mixin(value = TFGLayers.class, remap = false)
public class TfgLayersMixin {

  @Inject(
    method = "createRegionBiomeLayer",
    at = @At("HEAD"),
    cancellable = true
  )
  private static void tfcrealworld$regionBiomeLayerFromMap(
    RegionGenerator generator,
    long worldSeed,
    CallbackInfoReturnable<AreaFactory> cir
  ) {
    final AreaFactory layers = MapBiomeLayers.create(
      generator,
      Backends.current(),
      worldSeed
    );
    if (layers != null) {
      cir.setReturnValue(layers);
    }
  }
}
