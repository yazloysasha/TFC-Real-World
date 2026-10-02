package net.yazloysasha.tfcrealworld.mixin.world.surface.builder;

import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.ShoreAndOceanSurfaceBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ShoreAndOceanSurfaceBuilder.class, remap = false)
public interface ShoreAndOceanSurfaceBuilderAccessor {
  @Invoker("placeSeaIce")
  void tfcrealworld$invokePlaceSeaIce(
    SurfaceBuilderContext context,
    int x,
    int z,
    int seaLevel,
    float maxAnnualTemperature
  );
}
