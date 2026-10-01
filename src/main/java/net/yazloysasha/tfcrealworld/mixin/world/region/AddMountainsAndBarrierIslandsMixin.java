package net.yazloysasha.tfcrealworld.mixin.world.region;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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

/**
 * With {@code tectonics.png}, mountain ranges and volcanic arcs are real data,
 * so the random vanilla ranges and shelf arcs are suppressed. Barrier islands
 * stay procedural. The calls are wrapped, not redirected, so another mod may
 * redirect them too (TFC Mantle Mountains does).
 */
@Mixin(value = AddMountainsAndBarrierIslands.class, remap = false)
public abstract class AddMountainsAndBarrierIslandsMixin {

  @WrapOperation(
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
    Operation<IntSet> original,
    @Local(argsOnly = true) RegionGenerator.Context context
  ) {
    if (TectonicsRegistry.isActive(context.generator())) {
      return IntSets.EMPTY_SET;
    }
    return original.call(instance, region, random, originIndex);
  }

  @WrapOperation(
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
    Operation<IntSet> original,
    @Local(argsOnly = true) RegionGenerator.Context context
  ) {
    if (TectonicsRegistry.isActive(context.generator())) {
      return IntSets.EMPTY_SET;
    }
    return original.call(instance, region, random, originIndex);
  }
}
