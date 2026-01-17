package net.yazloysasha.tfcrealworld.mixin.world.settings;

import net.dries007.tfc.world.settings.Settings;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Settings.class, remap = false)
public class SettingsMixin {

  @Inject(method = "spawnCenterX", at = @At("RETURN"), cancellable = true)
  private void tfcrealworld$overrideSpawnCenterX(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.SPAWN_CENTER_X.get());
  }

  @Inject(method = "spawnCenterZ", at = @At("RETURN"), cancellable = true)
  private void tfcrealworld$overrideSpawnCenterZ(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.SPAWN_CENTER_Z.get());
  }

  @Inject(method = "spawnDistance", at = @At("RETURN"), cancellable = true)
  private void tfcrealworld$overrideSpawnDistance(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.SPAWN_DISTANCE.get());
  }

  @Inject(method = "flatBedrock", at = @At("RETURN"), cancellable = true)
  private void tfcrealworld$overrideFlatBedrock(
    CallbackInfoReturnable<Boolean> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.FLAT_BEDROCK.get());
  }

  @Inject(method = "continentalness", at = @At("RETURN"), cancellable = true)
  private void tfcrealworld$overrideContinentalness(
    CallbackInfoReturnable<Float> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.CONTINENTALNESS.get().floatValue());
  }

  @Inject(method = "grassDensity", at = @At("RETURN"), cancellable = true)
  private void tfcrealworld$overrideGrassDensity(
    CallbackInfoReturnable<Float> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.GRASS_DENSITY.get().floatValue());
  }

  @Inject(
    method = "temperatureConstant",
    at = @At("RETURN"),
    cancellable = true
  )
  private void tfcrealworld$overrideTemperatureConstant(
    CallbackInfoReturnable<Float> cir
  ) {
    cir.setReturnValue(
      TFCRealWorldConfig.TEMPERATURE_CONSTANT.get().floatValue()
    );
  }

  @Inject(method = "rainfallConstant", at = @At("RETURN"), cancellable = true)
  private void tfcrealworld$overrideRainfallConstant(
    CallbackInfoReturnable<Float> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.RAINFALL_CONSTANT.get().floatValue());
  }

  @Inject(method = "temperatureScale", at = @At("RETURN"), cancellable = true)
  private void tfcrealworld$overrideTemperatureScale(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.TEMPERATURE_SCALE.get());
  }

  @Inject(method = "rainfallScale", at = @At("RETURN"), cancellable = true)
  private void tfcrealworld$overrideRainfallScale(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.RAINFALL_SCALE.get());
  }
}
