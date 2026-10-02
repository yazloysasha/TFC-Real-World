package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddRiversAndLakes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.RiverEdge;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.RiversRegistry;
import net.yazloysasha.tfcrealworld.world.river.MapRivers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A profile with {@code rivers.bin} gives each region the real rivers that
 * lie in it. Without one, or with rivers from the map switched off, vanilla
 * grows its rivers.
 */
@Mixin(value = AddRiversAndLakes.class, remap = false)
public class AddRiversAndLakesMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$riversFromMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    final MapRivers rivers = RiversRegistry.get(context.generator());
    if (rivers != null) {
      rivers.addTo(context.region, context.generator().seed().seed());
      tfcrealworld$wetMapLakes(context.region);
      ci.cancel();
    }
  }

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$wetMapLakesAfterRivers(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    tfcrealworld$wetMapLakes(context.region);
  }

  /**
   * Vanilla makes the cell of every lake it places a little wetter. The
   * lakes of the map are not placed by vanilla, so they get the same here.
   */
  @Unique
  private static void tfcrealworld$wetMapLakes(Region region) {
    if (
      !TFCRealWorldConfig.CONTINENT_FROM_MAP.get() ||
      !TFCRealWorldConfig.LAKES_FROM_MAP.get()
    ) {
      return;
    }
    for (final Region.Point point : region.points()) {
      if (point.lake()) {
        point.rainfall +=
          MapRivers.RAINFALL_SHARE * (MapRivers.MAX_RAINFALL - point.rainfall);
      }
    }
  }

  /**
   * With lakes from the map its lake pixels are the lakes; vanilla's lakes
   * at river sources are skipped.
   */
  @Inject(method = "placeLakeNear", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$skipProceduralLakes(
    Region region,
    RiverEdge edge,
    int offsetX,
    int offsetZ,
    CallbackInfo ci
  ) {
    if (
      TFCRealWorldConfig.CONTINENT_FROM_MAP.get() &&
      TFCRealWorldConfig.LAKES_FROM_MAP.get()
    ) {
      ci.cancel();
    }
  }
}
