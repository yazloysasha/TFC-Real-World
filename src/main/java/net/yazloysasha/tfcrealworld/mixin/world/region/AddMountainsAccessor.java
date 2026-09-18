package net.yazloysasha.tfcrealworld.mixin.world.region;

import it.unimi.dsi.fastutil.ints.IntSet;
import net.dries007.tfc.world.region.AddMountains;
import net.dries007.tfc.world.region.Region;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AddMountains.class, remap = false)
public interface AddMountainsAccessor {
  @Invoker("placeRange")
  IntSet tfcrealworld$invokePlaceRange(
    Region region,
    RandomSource random,
    int originIndex
  );
}
