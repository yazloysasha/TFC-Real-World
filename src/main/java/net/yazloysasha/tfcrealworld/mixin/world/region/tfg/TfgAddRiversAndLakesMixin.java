package net.yazloysasha.tfcrealworld.mixin.world.region.tfg;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.RiverEdge;
import net.yazloysasha.tfcrealworld.world.region.MapRegionTasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.terrafirmagreg.core.world.new_ow_wg.region.TFGAddRiversAndLakes;

/**
 * A profile with {@code rivers.bin} gives each region the real rivers that
 * lie in it. Without one, or with rivers from the map switched off, the
 * generator grows its rivers.
 */
@Mixin(value = TFGAddRiversAndLakes.class, remap = false)
public class TfgAddRiversAndLakesMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$riversFromMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (MapRegionTasks.rivers(context)) {
      ci.cancel();
    }
  }

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$wetMapLakesAfterRivers(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    MapRegionTasks.wetMapLakes(context);
  }

  /** Lakes from the map: the generator's lakes at river sources are skipped. */
  @Inject(method = "placeLakeNear", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$skipProceduralLakes(
    Region region,
    RiverEdge edge,
    int offsetX,
    int offsetZ,
    CallbackInfo ci
  ) {
    if (MapRegionTasks.lakesFromMap()) {
      ci.cancel();
    }
  }
}
