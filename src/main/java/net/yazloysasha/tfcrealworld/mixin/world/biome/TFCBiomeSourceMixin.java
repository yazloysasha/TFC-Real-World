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

/**
 * The world is made by the region generator, as in TFC 3, and its settings
 * come from the config.
 */
@Mixin(value = TFCBiomeSource.class, remap = false)
public class TFCBiomeSourceMixin {

  @Inject(
    method = "defaultBiomeSource(JLnet/minecraft/core/Registry;)Lnet/dries007/tfc/world/biome/TFCBiomeSource;",
    at = @At("RETURN"),
    cancellable = true
  )
  private static void tfcrealworld$useRegionBiomeSource(
    long seed,
    Registry<Biome> biomeRegistry,
    CallbackInfoReturnable<TFCBiomeSource> cir
  ) {
    final TFCBiomeSource original = cir.getReturnValue();
    if (original != null && !(original instanceof RegionBiomeSource)) {
      cir.setReturnValue(
        new RegionBiomeSource(original.settings(), biomeRegistry)
      );
    }
  }

  @Inject(
    method = "settings()Lnet/dries007/tfc/world/biome/BiomeSourceExtension$Settings;",
    at = @At("RETURN"),
    cancellable = true
  )
  private void tfcrealworld$overrideSettings(
    CallbackInfoReturnable<BiomeSourceExtension.Settings> cir
  ) {
    final BiomeSourceExtension.Settings original = cir.getReturnValue();
    if (original == null) {
      return;
    }
    cir.setReturnValue(
      new BiomeSourceExtension.Settings(
        original.seed(),
        SpawnCenterHelper.getSpawnDistance(),
        SpawnCenterHelper.getSpawnCenterX(),
        SpawnCenterHelper.getSpawnCenterZ(),
        original.rockLayerSettings(),
        new ClimateSettings(
          TFCRealWorldConfig.TEMPERATURE_SCALE.get(),
          original.temperatureSettings().endlessPoles()
        ),
        new ClimateSettings(
          TFCRealWorldConfig.RAINFALL_SCALE.get(),
          original.rainfallSettings().endlessPoles()
        )
      )
    );
  }
}
