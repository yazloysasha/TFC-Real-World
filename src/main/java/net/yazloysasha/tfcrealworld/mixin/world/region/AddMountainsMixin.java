package net.yazloysasha.tfcrealworld.mixin.world.region;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.dries007.tfc.world.region.AddMountains;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.minecraft.util.RandomSource;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * With {@code tectonics.png}, mountain ranges and volcanic arcs are real
 * data, so the random ranges and shelf arcs are suppressed. Barrier islands
 * stay procedural. TerraFirmaEarth rewrites the task around methods of its
 * own, which are named here beside TFC 3's.
 */
@Mixin(value = AddMountains.class, remap = false, priority = 1500)
public class AddMountainsMixin {

  /** Whether the region being worked on takes its ranges from the map. */
  @Unique
  private static final ThreadLocal<Boolean> FROM_MAP = ThreadLocal.withInitial(
    () -> false
  );

  @Inject(method = "apply", at = @At("HEAD"))
  private void tfcrealworld$findTectonics(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    FROM_MAP.set(TectonicsRegistry.isActive(context.generator()));
  }

  @Inject(
    method = { "placeRange", "tfe$placeRange", "tfe$placeVolcanicArc" },
    at = @At("HEAD"),
    cancellable = true,
    require = 1
  )
  private void tfcrealworld$skipRandomRangesAndArcs(
    Region region,
    RandomSource random,
    int originIndex,
    CallbackInfoReturnable<IntSet> cir
  ) {
    if (FROM_MAP.get()) {
      cir.setReturnValue(IntSets.EMPTY_SET);
    }
  }
}
