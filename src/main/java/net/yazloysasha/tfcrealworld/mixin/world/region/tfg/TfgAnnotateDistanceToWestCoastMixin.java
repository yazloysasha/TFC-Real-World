package net.yazloysasha.tfcrealworld.mixin.world.region.tfg;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalWestCoastDistanceCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.terrafirmagreg.core.world.new_ow_wg.region.IRegionPoint;
import su.terrafirmagreg.core.world.new_ow_wg.region.TFGAnnotateDistanceToWestCoast;

@Mixin(value = TFGAnnotateDistanceToWestCoast.class, remap = false)
public class TfgAnnotateDistanceToWestCoastMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideDistanceToWestCoast(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return;
    }
    final GlobalWestCoastDistanceCache cache =
      GlobalWestCoastDistanceCache.getInstance();
    if (cache == null) {
      return;
    }

    final Region region = context.region;
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      ((IRegionPoint) point).tfg$setDistanceToWestCoast(
          cache.getDistance(
            RegionCoords.gridX(region, index),
            RegionCoords.gridZ(region, index)
          )
        );
    }
    ci.cancel();
  }
}
