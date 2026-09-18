package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.river.River;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
  targets = "net.dries007.tfc.world.region.AddRiversAndLakes$RegionRiverGenerator",
  remap = false
)
public class RegionRiverGeneratorMixin {

  @Unique
  private static final float NO_RIVERS_RAINFALL_THRESHOLD_MM = 75f;

  private static boolean tfcrealworld$excludesRiversByRainfall(
    @Nullable Region.Point point
  ) {
    return (
      point != null &&
      point.land() &&
      point.rainfall < NO_RIVERS_RAINFALL_THRESHOLD_MM
    );
  }

  @Inject(method = "isLegal", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$blockDryRiverSteps(
    River.Vertex prev,
    River.Vertex vertex,
    CallbackInfoReturnable<Boolean> cir
  ) {
    final RegionRiverGeneratorAccessor self =
      (RegionRiverGeneratorAccessor) (Object) this;
    final int gridX = (int) Math.round(vertex.x());
    final int gridZ = (int) Math.round(vertex.y());
    if (
      tfcrealworld$excludesRiversByRainfall(
        self.tfcrealworld$getRegion().at(gridX, gridZ)
      )
    ) {
      cir.setReturnValue(false);
    }
  }
}
