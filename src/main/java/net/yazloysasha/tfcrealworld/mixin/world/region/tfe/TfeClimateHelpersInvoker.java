package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import com.newterraearth.tfe.world.NTE121ClimateHelpers;
import net.dries007.tfc.world.region.RegionGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = NTE121ClimateHelpers.class, remap = false)
public interface TfeClimateHelpersInvoker {
  @Invoker("getPointRainVariance")
  static float tfcrealworld$invokeGetPointRainVariance(
    long levelSeed,
    RegionGenerator generator,
    int temperatureScale,
    int gridX,
    int gridZ
  ) {
    throw new AssertionError();
  }
}
