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
  private void realworld$overrideFlatBedrock(
    CallbackInfoReturnable<Boolean> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.FLAT_BEDROCK.get());
  }

  @Inject(
    method = "spawnDistance",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void realworld$overrideSpawnDistance(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.SPAWN_DISTANCE.get());
  }

  @Inject(
    method = "spawnCenterX",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void realworld$overrideSpawnCenterX(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.SPAWN_CENTER_X.get());
  }

  @Inject(
    method = "spawnCenterZ",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void realworld$overrideSpawnCenterZ(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.SPAWN_CENTER_Z.get());
  }

  @Inject(
    method = "temperatureScale",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void realworld$overrideTemperatureScale(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.TEMPERATURE_SCALE.get());
  }

  @Inject(
    method = "rainfallScale",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void realworld$overrideRainfallScale(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.RAINFALL_SCALE.get());
  }

  @Inject(
    method = "continentalness",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void realworld$overrideContinentalness(
    CallbackInfoReturnable<Float> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.CONTINENTALNESS.get().floatValue());
  }

  @Inject(
    method = "grassDensity",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void realworld$overrideGrassDensity(
    CallbackInfoReturnable<Float> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.GRASS_DENSITY.get().floatValue());
  }

  @Inject(
    method = "finiteContinents",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void realworld$overrideFiniteContinents(
    CallbackInfoReturnable<Boolean> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.FINITE_CONTINENTS.get());
  }
}
