package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddRiversAndLakes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RiverEdge;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.region.DryLand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AddRiversAndLakes.class, remap = false)
public class AddRiversAndLakesMixin {

  @Redirect(
    method = "createInitialDrains",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/Region$Point;shore()Z"
    )
  )
  private boolean tfcrealworld$skipDryShoreRiverSources(Region.Point point) {
    return (
      point.shore() &&
      !DryLand.isDrierThan(point, DryLand.RIVER_SOURCE_MIN_RAINFALL)
    );
  }

  @Inject(method = "setRiver", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$skipDryRiverCells(
    @Nullable Region.Point point,
    CallbackInfo ci
  ) {
    if (DryLand.isDrierThan(point, DryLand.RIVER_SOURCE_MIN_RAINFALL)) {
      ci.cancel();
    }
  }

  /**
   * When continents come from the map, lake pixels are authoritative
   * ({@code setLake} in AddContinents). Skip procedural placeLakeNear so the
   * map wins.
   */
  @Inject(method = "placeLakeNear", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$skipProceduralOrDryLakeNear(
    Region region,
    RiverEdge edge,
    int offsetX,
    int offsetZ,
    CallbackInfo ci
  ) {
    if (TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      ci.cancel();
      return;
    }
    final int gridX = (int) (edge.source().x() + 0.3f * offsetX);
    final int gridZ = (int) (edge.source().y() + 0.3f * offsetZ);
    if (
      DryLand.isDrierThan(
        region.at(gridX, gridZ),
        DryLand.RIVER_SOURCE_MIN_RAINFALL
      )
    ) {
      ci.cancel();
    }
  }
}
