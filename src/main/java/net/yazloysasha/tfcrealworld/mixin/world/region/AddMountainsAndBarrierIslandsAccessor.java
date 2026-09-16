package net.yazloysasha.tfcrealworld.mixin.world.region;

import it.unimi.dsi.fastutil.ints.IntSet;
import net.dries007.tfc.world.region.AddMountainsAndBarrierIslands;
import net.dries007.tfc.world.region.Region;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AddMountainsAndBarrierIslands.class, remap = false)
public interface AddMountainsAndBarrierIslandsAccessor {
  @Invoker("placeRange")
  IntSet tfcrealworld$invokePlaceRange(
    Region region,
    RandomSource random,
    int originIndex
  );

  @Invoker("placeBarrier")
  IntSet tfcrealworld$invokePlaceBarrier(
    Region region,
    RandomSource random,
    int originIndex
  );

  @Invoker("placeVolcanicArc")
  IntSet tfcrealworld$invokePlaceVolcanicArc(
    Region region,
    RandomSource random,
    int originIndex
  );
}
