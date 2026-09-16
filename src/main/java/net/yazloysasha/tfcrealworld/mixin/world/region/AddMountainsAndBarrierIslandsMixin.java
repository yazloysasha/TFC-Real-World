package net.yazloysasha.tfcrealworld.mixin.world.region;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.dries007.tfc.world.region.AddMountainsAndBarrierIslands;
import net.dries007.tfc.world.region.Region;
import net.minecraft.util.RandomSource;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * With {@code altitude_from_map}, heights come from PNG — skip TFC procedural mountain ranges only.
 * Volcanic arcs, barrier islands, and collisional marking stay vanilla (random small ocean chains).
 */
@Mixin(value = AddMountainsAndBarrierIslands.class, remap = false)
public class AddMountainsAndBarrierIslandsMixin {

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/AddMountainsAndBarrierIslands;placeRange(Lnet/dries007/tfc/world/region/Region;Lnet/minecraft/util/RandomSource;I)Lit/unimi/dsi/fastutil/ints/IntSet;"
    )
  )
  private IntSet tfcrealworld$skipProceduralMountainRangesWhenUsingAltitudeMap(
    AddMountainsAndBarrierIslands instance,
    Region region,
    RandomSource random,
    int originIndex
  ) {
    if (TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return new IntOpenHashSet();
    }
    return (
      (AddMountainsAndBarrierIslandsAccessor) (Object) instance
    ).tfcrealworld$invokePlaceRange(region, random, originIndex);
  }
}
