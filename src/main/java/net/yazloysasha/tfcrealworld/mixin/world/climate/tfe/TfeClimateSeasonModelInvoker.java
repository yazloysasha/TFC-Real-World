package net.yazloysasha.tfcrealworld.mixin.world.climate.tfe;

import com.newterraearth.tfe.world.NTEClimateSeasonModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = NTEClimateSeasonModel.class, remap = false)
public interface TfeClimateSeasonModelInvoker {
  @Invoker("signedSeasonalTemperatureAmplitude")
  static float tfcrealworld$invokeSignedSeasonalTemperatureAmplitude(
    int z,
    float temperatureScale
  ) {
    throw new AssertionError();
  }
}
