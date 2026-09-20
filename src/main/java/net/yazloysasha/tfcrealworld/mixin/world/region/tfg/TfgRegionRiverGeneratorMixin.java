package net.yazloysasha.tfcrealworld.mixin.world.region.tfg;

import net.dries007.tfc.world.river.River;
import net.yazloysasha.tfcrealworld.world.region.RiverRainfallExclusion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.terrafirmagreg.core.world.new_ow_wg.region.TFGAddRiversAndLakes$RegionRiverGenerator;

@Mixin(value = TFGAddRiversAndLakes$RegionRiverGenerator.class, remap = false)
public class TfgRegionRiverGeneratorMixin {

  @Inject(method = "isLegal", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$blockDryRiverSteps(
    River.Vertex prev,
    River.Vertex vertex,
    CallbackInfoReturnable<Boolean> cir
  ) {
    final TfgRegionRiverGeneratorAccessor self =
      (TfgRegionRiverGeneratorAccessor) (Object) this;
    final int gridX = (int) Math.round(vertex.x());
    final int gridZ = (int) Math.round(vertex.y());
    if (
      RiverRainfallExclusion.excludes(
        self.tfcrealworld$getRegion().maybeAt(gridX, gridZ)
      )
    ) {
      cir.setReturnValue(false);
    }
  }
}
