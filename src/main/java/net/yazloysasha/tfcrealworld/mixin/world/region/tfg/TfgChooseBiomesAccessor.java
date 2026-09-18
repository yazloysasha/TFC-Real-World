package net.yazloysasha.tfcrealworld.mixin.world.region.tfg;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import su.terrafirmagreg.core.world.new_ow_wg.region.TFGChooseBiomesTask;

@Mixin(value = TFGChooseBiomesTask.class, remap = false)
public interface TfgChooseBiomesAccessor {
  @Invoker("getHotSpotBiome")
  int tfcrealworld$invokeGetHotSpotBiome(int age);

  @Invoker("getBurrenBiome")
  int tfcrealworld$invokeGetBurrenBiome(int biome);

  @Invoker("getTowerKarstBiome")
  int tfcrealworld$invokeGetTowerKarstBiome(int biome);
}
