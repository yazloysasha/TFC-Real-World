package net.yazloysasha.tfcrealworld.mixin.world.surface.builder.tfe;

import com.newterraearth.tfe.world.surface.ShorelineSurfaceBuilder;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ShorelineSurfaceBuilder.class, remap = false)
public interface TfeShorelineSurfaceBuilderAccessor {
  @Invoker("frozenOceanExtension")
  void tfcrealworld$invokeFrozenOceanExtension(
    SurfaceBuilderContext context,
    int startY,
    int endY,
    int oceanFloorY,
    int seaLevel
  );
}
