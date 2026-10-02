package net.yazloysasha.tfcrealworld.mixin.world.biome;

import com.llamalad7.mixinextras.sugar.Local;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.settings.Settings;
import net.minecraft.core.QuartPos;
import net.yazloysasha.tfcrealworld.util.helpers.SpawnCenterHelper;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BiomeSourceExtension.class, remap = false)
public interface BiomeSourceExtensionMixin {
  /** At sea there is no river biome, whatever shore biome stands there. */
  @Redirect(
    method = "getBiomeExtension",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/biome/BiomeExtension;hasRivers()Z"
    )
  )
  private static boolean tfcrealworld$noRiverBiomeAtSea(
    BiomeExtension biome,
    @Local(argsOnly = true, ordinal = 0) int quartX,
    @Local(argsOnly = true, ordinal = 1) int quartZ
  ) {
    return (
      biome.hasRivers() &&
      !ContinentNoiseRegistry.isSeaAtBlock(
        QuartPos.toBlock(quartX),
        QuartPos.toBlock(quartZ)
      )
    );
  }

  @Redirect(
    method = "findSpawnBiome",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/settings/Settings;spawnDistance()I"
    )
  )
  private static int tfcrealworld$redirectSpawnDistance(Settings settings) {
    return SpawnCenterHelper.getSpawnDistance();
  }

  @Redirect(
    method = "findSpawnBiome",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/settings/Settings;spawnCenterX()I"
    )
  )
  private static int tfcrealworld$redirectSpawnCenterX(Settings settings) {
    return SpawnCenterHelper.getSpawnCenterX();
  }

  @Redirect(
    method = "findSpawnBiome",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/settings/Settings;spawnCenterZ()I"
    )
  )
  private static int tfcrealworld$redirectSpawnCenterZ(Settings settings) {
    return SpawnCenterHelper.getSpawnCenterZ();
  }
}
