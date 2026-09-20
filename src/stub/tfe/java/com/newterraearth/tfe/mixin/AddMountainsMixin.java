package com.newterraearth.tfe.mixin;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.minecraft.util.RandomSource;

public abstract class AddMountainsMixin {

  public void apply(RegionGenerator.Context context) {}

  private IntSet tfe$placeRange(
    Region region,
    RandomSource random,
    int originIndex
  ) {
    return new IntOpenHashSet();
  }

  private IntSet tfe$placeVolcanicArc(
    Region region,
    RandomSource random,
    int originIndex
  ) {
    return new IntOpenHashSet();
  }

  private IntSet tfe$placeBarrier(
    Region region,
    RandomSource random,
    int originIndex
  ) {
    return new IntOpenHashSet();
  }
}
