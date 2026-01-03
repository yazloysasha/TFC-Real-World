package net.yazloysasha.tfcrealworld.mixin.world.settings;

import net.dries007.tfc.world.settings.Settings;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Settings.class)
public class SettingsMixin {

  @Inject(
    method = "flatBedrock",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideFlatBedrock(
    CallbackInfoReturnable<Boolean> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.getFlatBedrock());
  }

  @Inject(
    method = "spawnDistance",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideSpawnDistance(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.getSpawnDistance());
  }

  @Inject(
    method = "spawnCenterX",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideSpawnCenterX(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.getSpawnCenterX());
  }

  @Inject(
    method = "spawnCenterZ",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideSpawnCenterZ(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.getSpawnCenterZ());
  }

  @Inject(
    method = "temperatureScale",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideTemperatureScale(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.getTemperatureScale());
  }

  @Inject(
    method = "rainfallScale",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideRainfallScale(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.getRainfallScale());
  }

  @Inject(
    method = "continentalness",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideContinentalness(
    CallbackInfoReturnable<Float> cir
  ) {
    cir.setReturnValue((float) TFCRealWorldConfig.getContinentalness());
  }

  @Inject(
    method = "grassDensity",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideGrassDensity(
    CallbackInfoReturnable<Float> cir
  ) {
    cir.setReturnValue((float) TFCRealWorldConfig.getGrassDensity());
  }

  @Inject(
    method = "finiteContinents",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideFiniteContinents(
    CallbackInfoReturnable<Boolean> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.getFiniteContinents());
  }
}
