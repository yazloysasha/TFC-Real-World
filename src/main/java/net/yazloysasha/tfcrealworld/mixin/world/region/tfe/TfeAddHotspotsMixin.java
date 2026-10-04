package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import com.newterraearth.tfe.world.region.NTEAddHotspots;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.region.MapRegionTasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hotspots where the tectonics map has them. */
@Mixin(value = NTEAddHotspots.class, remap = false)
public class TfeAddHotspotsMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$hotspotsFromMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (MapRegionTasks.hotspots(context)) {
      ci.cancel();
    }
  }
}
