package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.river.RiverFractal;
import net.yazloysasha.tfcrealworld.world.region.RiverRainfallExclusion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
  targets = "net.dries007.tfc.world.region.AddRiversAndLakes$RegionRiverGenerator",
  remap = false
)
public class RegionRiverGeneratorMixin {

  @Inject(method = "isLegal", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$blockDryRiverSteps(
    RiverFractal.Vertex prev,
    RiverFractal.Vertex vertex,
    CallbackInfoReturnable<Boolean> cir
  ) {
    final RegionRiverGeneratorAccessor self =
      (RegionRiverGeneratorAccessor) (Object) this;
    final int gridX = Math.round(vertex.x());
    final int gridZ = Math.round(vertex.y());
    if (
      RiverRainfallExclusion.excludes(
        self.tfcrealworld$getRegion().maybeAt(gridX, gridZ)
      )
    ) {
      cir.setReturnValue(false);
    }
  }
}
