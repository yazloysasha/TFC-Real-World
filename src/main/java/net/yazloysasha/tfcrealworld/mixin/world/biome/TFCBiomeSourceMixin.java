package net.yazloysasha.tfcrealworld.mixin.world.biome;

import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.biome.RegionBiomeSource;
import net.dries007.tfc.world.biome.TFCBiomeSource;
import net.dries007.tfc.world.settings.ClimateSettings;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.SpawnCenterHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TFCBiomeSource.class, remap = false)
public class TFCBiomeSourceMixin {

  @Inject(
    method = "defaultBiomeSource(JLnet/minecraft/core/Registry;)Lnet/dries007/tfc/world/biome/TFCBiomeSource;",
    at = @At("RETURN"),
    cancellable = true
  )
  private static void tfcrealworld$useRegionBiomeSourceWhenContinentsFromMap(
    long seed,
    Registry<Biome> biomeRegistry,
    CallbackInfoReturnable<TFCBiomeSource> cir
  ) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return;
    }

    final TFCBiomeSource original = cir.getReturnValue();
    if (original == null || original instanceof RegionBiomeSource) {
      return;
    }

    cir.setReturnValue(
      new RegionBiomeSource(original.settings(), biomeRegistry)
    );
  }

  @Inject(
    method = "settings()Lnet/dries007/tfc/world/biome/BiomeSourceExtension$Settings;",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideSettings(
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
