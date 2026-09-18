package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddRiversAndLakes;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.region.RiverRainfallExclusion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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

  @Redirect(
    method = "annotateRiver",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/Region$Point;setLake()V"
    )
  )
  private void tfcrealworld$setLakeIfNotDry(Region.Point point) {
    if (!RiverRainfallExclusion.excludes(point)) {
      point.setLake();
    }
  }
}
