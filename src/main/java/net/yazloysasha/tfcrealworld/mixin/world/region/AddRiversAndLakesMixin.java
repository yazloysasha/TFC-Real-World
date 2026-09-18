package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddRiversAndLakes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RiverEdge;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AddRiversAndLakes.class, remap = false)
public class AddRiversAndLakesMixin {

  @Unique
  private static final float NO_RIVERS_RAINFALL_THRESHOLD_MM = 100f;

  private static boolean tfcrealworld$excludesRiversByRainfall(
    @Nullable Region.Point point
  ) {
    return (
      point != null &&
      point.land() &&
      point.rainfall < NO_RIVERS_RAINFALL_THRESHOLD_MM
    );
  }

  @Redirect(
    method = "createInitialDrains",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/Region$Point;shore()Z"
    )
  )
  private boolean tfcrealworld$skipDryShoreRiverSources(Region.Point point) {
    return point.shore() && !tfcrealworld$excludesRiversByRainfall(point);
  }

  @Inject(method = "setRiver", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$skipDryRiverCells(
    @Nullable Region.Point point,
    CallbackInfo ci
  ) {
    if (tfcrealworld$excludesRiversByRainfall(point)) {
      ci.cancel();
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
    if (tfcrealworld$excludesRiversByRainfall(region.at(gridX, gridZ))) {
      ci.cancel();
    }
  }
}
