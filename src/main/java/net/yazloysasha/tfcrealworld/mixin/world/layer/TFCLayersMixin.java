package net.yazloysasha.tfcrealworld.mixin.world.layer;

import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.backend.Backends;
import net.yazloysasha.tfcrealworld.world.layer.MapBiomeLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Region biome layers that follow the coastline of the continent map. */
@Mixin(value = TFCLayers.class, remap = false, priority = 1500)
public class TFCLayersMixin {

  @Inject(
    method = "createRegionBiomeLayer",
    at = @At("HEAD"),
    cancellable = true
  )
  private static void tfcrealworld$regionBiomeLayerFromMap(
    RegionGenerator generator,
    long seed,
    CallbackInfoReturnable<AreaFactory> cir
  ) {
    final AreaFactory layers = MapBiomeLayers.create(
      generator,
      Backends.current(),
      seed
    );
    if (layers != null) {
      cir.setReturnValue(layers);
    }
  }
}
