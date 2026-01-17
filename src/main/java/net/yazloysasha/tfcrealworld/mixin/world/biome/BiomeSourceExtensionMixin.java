package net.yazloysasha.tfcrealworld.mixin.world.biome;

import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.settings.Settings;
import net.yazloysasha.tfcrealworld.util.helpers.SpawnCenterHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BiomeSourceExtension.class)
public abstract class BiomeSourceExtensionMixin {

  @Redirect(
    method = "findSpawnBiome",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/settings/Settings;spawnDistance()I",
      remap = false
    ),
    remap = false
  )
  private static int tfcrealworld$redirectSpawnDistance(Settings settings) {
    return SpawnCenterHelper.getSpawnDistance();
  }

  @Redirect(
    method = "findSpawnBiome",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/settings/Settings;spawnCenterX()I",
      remap = false
    ),
    remap = false
  )
  private static int tfcrealworld$redirectSpawnCenterX(Settings settings) {
    return SpawnCenterHelper.getSpawnCenterX();
  }

  @Redirect(
    method = "findSpawnBiome",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/settings/Settings;spawnCenterZ()I",
      remap = false
    ),
    remap = false
  )
  private static int tfcrealworld$redirectSpawnCenterZ(Settings settings) {
    return SpawnCenterHelper.getSpawnCenterZ();
  }
}
