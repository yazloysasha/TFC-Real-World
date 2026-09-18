package net.yazloysasha.tfcrealworld.mixin.world.region;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.dries007.tfc.world.region.AddMountains;
import net.dries007.tfc.world.region.Region;
import net.minecraft.util.RandomSource;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Same rule as 1.21.1 {@code AddMountainsAndBarrierIslandsMixin}: when altitude
 * comes from the map, skip procedural contour ranges and let
 * {@code AnnotateBiomeAltitudeMixin} mark mountain cores. TFC 3 has no
 * {@code placeBarrier}/{@code placeVolcanicArc}; those stay with TFG/TFC if
 * they appear later.
 */
@Mixin(value = AddMountains.class, remap = false)
public class AddMountainsMixin {

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/AddMountains;placeRange(Lnet/dries007/tfc/world/region/Region;Lnet/minecraft/util/RandomSource;I)Lit/unimi/dsi/fastutil/ints/IntSet;"
    )
  )
  private IntSet tfcrealworld$skipProceduralMountainRangesWhenUsingAltitudeMap(
    AddMountains instance,
    Region region,
    RandomSource random,
    int originIndex
  ) {
    if (TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return new IntOpenHashSet();
    }
    return (
      (AddMountainsAccessor) (Object) instance
    ).tfcrealworld$invokePlaceRange(region, random, originIndex);
  }
}
