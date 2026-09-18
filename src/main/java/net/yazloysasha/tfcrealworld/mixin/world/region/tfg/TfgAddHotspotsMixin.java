package net.yazloysasha.tfcrealworld.mixin.world.region.tfg;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotBiomes;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.terrafirmagreg.core.world.new_ow_wg.region.IRegionPoint;
import su.terrafirmagreg.core.world.new_ow_wg.region.TFGAddHotspots;

@Mixin(value = TFGAddHotspots.class, remap = false)
public class TfgAddHotspotsMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$applyHotspotsFromMapOnly(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      return;
    }
    if (HotspotsNoiseRegistry.get(context.generator()) == null) {
      return;
    }
    final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
    if (layout == null) {
      return;
    }

    final Region region = context.region;
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      final byte mapAge = layout.ageAtGrid(
        RegionCoords.gridX(region, index),
        RegionCoords.gridZ(region, index)
      );
      if (mapAge > 0) {
        ((IRegionPoint) point).tfg$setHotSpotAge(mapAge);
        if (MapHotspotBiomes.shouldSetLandForMapAge(mapAge)) {
          point.setLand();
        }
      }
    }
    ci.cancel();
  }
}
