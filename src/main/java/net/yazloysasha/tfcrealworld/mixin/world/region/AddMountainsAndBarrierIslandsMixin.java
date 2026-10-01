package net.yazloysasha.tfcrealworld.mixin.world.region;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.dries007.tfc.world.region.AddMountainsAndBarrierIslands;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.minecraft.util.RandomSource;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * With {@code tectonics.png}, mountain ranges and volcanic arcs are real data,
 * so the random vanilla ranges and shelf arcs are suppressed. Barrier islands
 * stay procedural.
 */
@Mixin(value = AddMountainsAndBarrierIslands.class, remap = false)
public abstract class AddMountainsAndBarrierIslandsMixin {

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/AddMountainsAndBarrierIslands;placeRange(Lnet/dries007/tfc/world/region/Region;Lnet/minecraft/util/RandomSource;I)Lit/unimi/dsi/fastutil/ints/IntSet;"
    )
  )
  private IntSet tfcrealworld$skipRandomRanges(
    AddMountainsAndBarrierIslands instance,
    Region region,
    RandomSource random,
    int originIndex,
    @Local(argsOnly = true) RegionGenerator.Context context
  ) {
    if (TectonicsRegistry.isActive(context.generator())) {
      return IntSets.EMPTY_SET;
    }
    return (
      (AddMountainsAndBarrierIslandsAccessor) (Object) instance
    ).tfcrealworld$invokePlaceRange(region, random, originIndex);
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/AddMountainsAndBarrierIslands;placeVolcanicArc(Lnet/dries007/tfc/world/region/Region;Lnet/minecraft/util/RandomSource;I)Lit/unimi/dsi/fastutil/ints/IntSet;"
    )
  )
  private IntSet tfcrealworld$skipRandomArcs(
    AddMountainsAndBarrierIslands instance,
    Region region,
    RandomSource random,
    int originIndex,
    @Local(argsOnly = true) RegionGenerator.Context context
  ) {
    if (TectonicsRegistry.isActive(context.generator())) {
      return IntSets.EMPTY_SET;
    }
    return (
      (AddMountainsAndBarrierIslandsAccessor) (Object) instance
    ).tfcrealworld$invokePlaceVolcanicArc(region, random, originIndex);
  }
}
