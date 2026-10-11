package net.yazloysasha.tfcrealworld.mixin.world.surface.tfe;

import com.newterraearth.tfe.world.surface.ShorelineSurfaceBuilder;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.yazloysasha.tfcrealworld.world.backend.Backends;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Sea ice and icebergs are laid by {@code TfeFrozenWaterSurfaceBuilder} for
 * every biome alike, so the shore and ocean builder leaves them to it.
 */
@Mixin(value = ShorelineSurfaceBuilder.class, remap = false)
public class TfeShorelineSurfaceBuilderMixin {

  @Redirect(
    method = "buildSurface",
    at = @At(
      value = "INVOKE",
      target = "Lcom/newterraearth/tfe/world/surface/ShorelineSurfaceBuilder;frozenOceanExtension(Lnet/dries007/tfc/world/surface/SurfaceBuilderContext;IIII)V"
    )
  )
  private void tfcrealworld$leaveIceToEveryBiome(
    ShorelineSurfaceBuilder instance,
    SurfaceBuilderContext context,
    int startY,
    int endY,
    int oceanFloorY,
    int seaLevel
  ) {
    if (!Backends.current().freezesAllOpenWater()) {
      final TfeShorelineSurfaceBuilderAccessor ice =
        (TfeShorelineSurfaceBuilderAccessor) instance;
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
