package net.yazloysasha.tfcrealworld.mixin.world.region.tfg;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RiverEdge;
import net.yazloysasha.tfcrealworld.world.region.RiverRainfallExclusion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.terrafirmagreg.core.world.new_ow_wg.region.TFGAddRiversAndLakes;

@Mixin(value = TFGAddRiversAndLakes.class, remap = false)
public class TfgAddRiversAndLakesMixin {

  @Redirect(
    method = "createInitialDrains",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/Region$Point;shore()Z"
    )
  )
  private boolean tfcrealworld$skipDryShoreRiverSources(Region.Point point) {
    return point.shore() && !RiverRainfallExclusion.excludes(point);
  }

  @Redirect(
    method = "createInitialDrains",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/Region$Point;setRiver()V"
    )
  )
  private void tfcrealworld$setRiverIfNotDry(Region.Point point) {
    if (!RiverRainfallExclusion.excludes(point)) {
      point.setRiver();
    }
  }

  @Inject(method = "placeLakeNear", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$skipDryLakeNear(
    Region region,
    RiverEdge edge,
    int offsetX,
    int offsetZ,
    CallbackInfo ci
  ) {
    final int gridX = (int) (edge.source().x() + 0.3f * offsetX);
    final int gridZ = (int) (edge.source().y() + 0.3f * offsetZ);
    if (RiverRainfallExclusion.excludes(region.maybeAt(gridX, gridZ))) {
      ci.cancel();
    }
  }
}
