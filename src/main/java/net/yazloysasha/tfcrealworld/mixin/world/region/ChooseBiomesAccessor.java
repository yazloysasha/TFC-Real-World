package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.ChooseBiomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ChooseBiomes.class, remap = false)
public interface ChooseBiomesAccessor {
  @Invoker("randomSeededFrom")
  int tfcrealworld$invokeRandomSeededFrom(
    long rngSeed,
    int areaSeed,
    int[] choices
  );
}
