package net.yazloysasha.tfcrealworld.mixin.world.biome;

import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.settings.ClimateSettings;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.SpawnCenterHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BiomeSourceExtension.class, remap = false)
public interface BiomeSourceExtensionMixin {
  @Inject(
    method = "settings()Lnet/dries007/tfc/world/biome/BiomeSourceExtension$Settings;",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  default void tfcrealworld$overrideSettings(
    CallbackInfoReturnable<BiomeSourceExtension.Settings> cir
  ) {
    BiomeSourceExtension.Settings original = cir.getReturnValue();
    if (original == null) {
      return;
    }

    int spawnDistance = SpawnCenterHelper.getSpawnDistance();
    int spawnCenterX = SpawnCenterHelper.getSpawnCenterX();
    int spawnCenterZ = SpawnCenterHelper.getSpawnCenterZ();

    int temperatureScale = TFCRealWorldConfig.TEMPERATURE_SCALE.get();
    int rainfallScale = TFCRealWorldConfig.RAINFALL_SCALE.get();

    ClimateSettings newTemperatureSettings = new ClimateSettings(
      temperatureScale,
      original.temperatureSettings().endlessPoles()
    );

    ClimateSettings newRainfallSettings = new ClimateSettings(
      rainfallScale,
      original.rainfallSettings().endlessPoles()
    );

    BiomeSourceExtension.Settings newSettings =
      new BiomeSourceExtension.Settings(
        original.seed(),
        spawnDistance,
        spawnCenterX,
        spawnCenterZ,
        original.rockLayerSettings(),
        newTemperatureSettings,
        newRainfallSettings
      );

    cir.setReturnValue(newSettings);
  }
}
