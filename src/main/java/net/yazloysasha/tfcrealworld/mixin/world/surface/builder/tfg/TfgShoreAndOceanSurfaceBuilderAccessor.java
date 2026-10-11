package net.yazloysasha.tfcrealworld.mixin.world.surface.builder.tfg;

import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import su.terrafirmagreg.core.world.new_ow_wg.surface_builders.ShoreAndOceanSurfaceBuilder;

@Mixin(value = ShoreAndOceanSurfaceBuilder.class, remap = false)
public interface TfgShoreAndOceanSurfaceBuilderAccessor {
  @Invoker("frozenOceanExtension")
  void tfcrealworld$invokeFrozenOceanExtension(
    SurfaceBuilderContext context,
    int startY,
    int endY,
    int oceanFloorY,
    int seaLevel
  );
}
