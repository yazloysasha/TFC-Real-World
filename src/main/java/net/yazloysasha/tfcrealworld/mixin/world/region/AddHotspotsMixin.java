package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddHotspots;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AddHotspots.class, remap = false)
public class AddHotspotsMixin {

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
    for (final var point : context.region.points()) {
      if (point == null) {
        continue;
      }
      final byte mapAge = layout.ageAtGrid(point.x, point.z);
      if (mapAge > 0) {
        point.hotSpotAge = mapAge;
        if (mapAge != 4) {
          point.setLand();
        }
      }
    }
    ci.cancel();
  }
}
