package net.yazloysasha.tfcrealworld.mixin.world.region;

import java.util.List;
import net.dries007.tfc.world.region.AddRiversAndLakes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.RiverEdge;
import net.yazloysasha.tfcrealworld.util.registry.RiversRegistry;
import net.yazloysasha.tfcrealworld.world.region.MapRegionTasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A profile with {@code rivers.bin} gives each region the real rivers that
 * lie in it. The task still runs to its end with them, for a generator that
 * goes on from there (TerraFirmaEarth lays its river valleys): it grows no
 * rivers of its own and takes the map's instead.
 */
@Mixin(value = AddRiversAndLakes.class, remap = false)
public class AddRiversAndLakesMixin {

  @Unique
  private static final ThreadLocal<RegionGenerator.Context> CONTEXT =
    new ThreadLocal<>();

  @Inject(method = "apply", at = @At("HEAD"))
  private void tfcrealworld$enter(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    CONTEXT.set(context);
  }

  @Inject(method = "createInitialDrains", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$growNoRivers(CallbackInfo ci) {
    if (RiversRegistry.get(CONTEXT.get().generator()) != null) {
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
    if (!MapRegionTasks.rivers(CONTEXT.get())) {
      region.setRivers(rivers);
    }
  }

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$leave(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    CONTEXT.remove();
    if (RiversRegistry.get(context.generator()) == null) {
      MapRegionTasks.wetMapLakes(context);
    }
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
