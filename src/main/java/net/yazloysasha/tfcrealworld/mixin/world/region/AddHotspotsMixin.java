package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddHotspots;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
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
    if (!TectonicsRegistry.isActive(context.generator())) {
      return;
    }
    final MapHotspotLayout layout = TectonicsRegistry.hotspotLayout();
    final PNGContinentNoise continent = ContinentNoiseRegistry.get(
      context.generator()
    );
    if (layout != null) {
      for (final var point : context.region.points()) {
        if (point == null) {
          continue;
        }
        final byte mapAge = layout.ageAtGrid(point.x, point.z);
        if (mapAge > 0) {
          point.hotSpotAge = mapAge;
          // Like vanilla, young hotspots build islands, but only where
          // continent.png has one; islands smaller than a cell would
          // otherwise lose their shield volcano. Seamounts stay ocean.
          if (
            mapAge < 4 &&
            !point.land() &&
            continent != null &&
            continent.anyNonOceanInCell(point.x, point.z)
          ) {
            point.setLand();
          }
        }
      }
    }
    ci.cancel();
  }
}
