package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.backport.TfeMapHotspots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TFE runs {@code NTEAddHotspots} from {@code Context.run} HEAD. Priority 500
 * TAIL runs after that call and overwrites procedural ages with the map.
 */
@Mixin(value = RegionGenerator.Context.class, remap = false, priority = 500)
public class TfeRegionGeneratorContextMixin {

  @Inject(method = "run", at = @At("TAIL"))
  private void tfcrealworld$replaceTfeHotspotsWithMap(
    RegionGenerator.Task task,
    CallbackInfo ci
  ) {
    if (task != RegionGenerator.Task.ANNOTATE_DISTANCE_TO_OCEAN) {
      return;
    }
    TfeMapHotspots.applyFromMap((RegionGenerator.Context) (Object) this);
  }
}
