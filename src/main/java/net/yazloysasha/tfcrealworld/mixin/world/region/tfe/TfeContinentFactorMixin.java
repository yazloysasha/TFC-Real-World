package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.DivergenceNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.backport.HotspotGeneratorNoises;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGDivergenceNoise;
import net.yazloysasha.tfcrealworld.world.region.FiniteContinentFactor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RegionGenerator.class, remap = false, priority = 500)
public abstract class TfeContinentFactorMixin {

  @Inject(method = "nte$continentFactor", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$continentFactorFromMap(
    int gridX,
    int gridZ,
    CallbackInfoReturnable<Float> cir
  ) {
    cir.setReturnValue(FiniteContinentFactor.atGrid(gridX, gridZ));
  }

  @Inject(method = "nte$getDivergence", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$applyDivergenceFromMap(
    int gridX,
    int gridZ,
    CallbackInfoReturnable<Double> cir
  ) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return;
    }
    if (!TFCRealWorldConfig.TECTONICS_FROM_MAP.get()) {
      return;
    }
    final PNGDivergenceNoise divergenceNoise = DivergenceNoiseRegistry.get(
      (RegionGenerator) (Object) this
    );
    if (divergenceNoise == null) {
      return;
    }
    cir.setReturnValue((double) divergenceNoise.getDivergence(gridX, gridZ));
  }

  @Inject(method = "<init>", at = @At("TAIL"))
  private void tfcrealworld$reapplyMapNoisesAfterTfeInit(CallbackInfo ci) {
    HotspotGeneratorNoises.reapplyMapNoises((RegionGenerator) (Object) this);
  }

  @Inject(method = "nte$setRootLevelSeed", at = @At("RETURN"))
  private void tfcrealworld$replaceMapNoisesAfterTfeReinit(
    long rootLevelSeed,
    CallbackInfo ci
  ) {
    HotspotGeneratorNoises.reapplyMapNoises((RegionGenerator) (Object) this);
  }
}
