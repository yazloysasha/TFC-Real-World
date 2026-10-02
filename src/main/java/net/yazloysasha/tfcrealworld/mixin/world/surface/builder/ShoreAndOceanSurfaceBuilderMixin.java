package net.yazloysasha.tfcrealworld.mixin.world.surface.builder;

import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.ShoreAndOceanSurfaceBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla's sea ice and icebergs are laid by {@code FrozenWaterSurfaceBuilder}
 * for every biome alike, so the shore and ocean builder leaves them to it.
 */
@Mixin(value = ShoreAndOceanSurfaceBuilder.class, remap = false)
public class ShoreAndOceanSurfaceBuilderMixin {

  @Redirect(
    method = "buildSurface",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/surface/builder/ShoreAndOceanSurfaceBuilder;frozenOceanExtension(Lnet/dries007/tfc/world/surface/SurfaceBuilderContext;IIII)V"
    )
  )
  private void tfcrealworld$leaveIceToEveryBiome(
    ShoreAndOceanSurfaceBuilder instance,
    SurfaceBuilderContext context,
    int startY,
    int endY,
    int oceanFloorY,
    int seaLevel
  ) {}
}
