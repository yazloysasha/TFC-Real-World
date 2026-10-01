package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddHotspots;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * With volcanoes from the map, hotspots stand where the tectonics map has
 * them instead of where vanilla's noise puts them. As in vanilla, every
 * hotspot but an ancient one raises land.
 */
@Mixin(value = AddHotspots.class, remap = false)
public class AddHotspotsMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$hotspotsFromMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!TectonicsRegistry.isActive(context.generator())) {
      return;
    }
    final MapHotspotLayout layout = TectonicsRegistry.hotspotLayout();
    if (layout == null) {
      return;
    }
    for (final var point : context.region.points()) {
      final byte age = layout.ageAtGrid(point.x, point.z);
      if (age > 0) {
        point.hotSpotAge = age;
        if (age != 4) {
          point.setLand();
        }
      }
    }
    ci.cancel();
  }
}
