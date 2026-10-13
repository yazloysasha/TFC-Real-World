package net.yazloysasha.tfcrealworld.mixin.world.region;

import java.util.List;
import net.dries007.tfc.world.region.AddRiversAndLakes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RiverEdge;
import net.yazloysasha.tfcrealworld.util.registry.RiversRegistry;
import net.yazloysasha.tfcrealworld.world.region.MapRegionTasks;
import net.yazloysasha.tfcrealworld.world.region.RegionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A profile with {@code rivers.bin} gives each region the real rivers that
 * lie in it: the generator grows no rivers of its own and takes the map's
 * instead.
 */
@Mixin(value = AddRiversAndLakes.class, remap = false)
public class AddRiversAndLakesMixin {

  @Inject(method = "createInitialDrains", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$growNoRivers(CallbackInfo ci) {
    if (RiversRegistry.get(RegionContext.current().generator()) != null) {
      ci.cancel();
    }
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/Region;setRivers(Ljava/util/List;)V"
    )
  )
  private void tfcrealworld$riversFromMap(
    Region region,
    List<RiverEdge> rivers
  ) {
    if (!MapRegionTasks.rivers(RegionContext.current())) {
      ((RegionAccessor) (Object) region).tfcrealworld$setRivers(rivers);
    }
  }

  /** Map rivers have their own source lakes: see MapRivers. */
  @Inject(method = "annotateRiver", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$noLakesOfGrownRivers(CallbackInfo ci) {
    if (RiversRegistry.get(RegionContext.current().generator()) != null) {
      ci.cancel();
    }
  }

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$leave(CallbackInfo ci) {
    final RegionContext context = RegionContext.current();
    if (RiversRegistry.get(context.generator()) == null) {
      MapRegionTasks.wetMapLakes(context);
    }
  }

  /** Lakes from the map: the generator's lakes at river sources are skipped. */
  @Redirect(
    method = "annotateRiver",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/Region$Point;setLake()V"
    )
  )
  private void tfcrealworld$skipProceduralLakes(Region.Point point) {
    if (!MapRegionTasks.lakesFromMap()) {
      point.setLake();
    }
  }
}
