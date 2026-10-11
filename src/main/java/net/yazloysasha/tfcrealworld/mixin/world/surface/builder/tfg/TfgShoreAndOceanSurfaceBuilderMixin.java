package net.yazloysasha.tfcrealworld.mixin.world.surface.builder.tfg;

import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.yazloysasha.tfcrealworld.world.backend.Backends;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import su.terrafirmagreg.core.world.new_ow_wg.surface_builders.ShoreAndOceanSurfaceBuilder;

/**
 * Sea ice and icebergs are laid by {@code FrozenWaterSurfaceBuilder} for
 * every biome alike, so the shore and ocean builder leaves them to it.
 */
@Mixin(value = ShoreAndOceanSurfaceBuilder.class, remap = false)
public class TfgShoreAndOceanSurfaceBuilderMixin {

  @Redirect(
    method = "buildSurface",
    at = @At(
      value = "INVOKE",
      target = "Lsu/terrafirmagreg/core/world/new_ow_wg/surface_builders/ShoreAndOceanSurfaceBuilder;frozenOceanExtension(Lnet/dries007/tfc/world/surface/SurfaceBuilderContext;IIII)V"
    )
  )
  private void tfcrealworld$leaveIceToEveryBiome(
    ShoreAndOceanSurfaceBuilder instance,
    SurfaceBuilderContext context,
    int startY,
    int endY,
    int oceanFloorY,
    int seaLevel
  ) {
    if (!Backends.current().freezesAllOpenWater()) {
      final TfgShoreAndOceanSurfaceBuilderAccessor ice =
        (TfgShoreAndOceanSurfaceBuilderAccessor) instance;
      ice.tfcrealworld$invokeFrozenOceanExtension(
        context,
        startY,
        endY,
        oceanFloorY,
        seaLevel
      );
    }
  }
}
