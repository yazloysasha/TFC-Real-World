package net.yazloysasha.tfcrealworld.mixin.world.surface.builder;

import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.ShoreAndOceanSurfaceBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ShoreAndOceanSurfaceBuilder.class, remap = false)
public interface ShoreAndOceanSurfaceBuilderAccessor {
  @Invoker("frozenOceanExtension")
  void tfcrealworld$invokeFrozenOceanExtension(
    SurfaceBuilderContext context,
    int startY,
    int endY,
    int oceanFloorY,
    int seaLevel
  );
}
