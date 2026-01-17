package net.yazloysasha.tfcrealworld.mixin.world.biome;

import net.dries007.tfc.TerraFirmaCraft;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.settings.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.RandomSource;
import net.yazloysasha.tfcrealworld.util.helpers.SpawnCenterHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = BiomeSourceExtension.class, remap = false)
public interface BiomeSourceExtensionMixin {
  /**
   * @author yazloysasha
   * @reason Mixin 0.8.5 does not support injectors for interface default methods
   */
  @Overwrite
  default BlockPos findSpawnBiome(Settings settings, RandomSource random) {
    final int step = Math.max(1, SpawnCenterHelper.getSpawnDistance() / 256);
    final int centerX = QuartPos.fromBlock(SpawnCenterHelper.getSpawnCenterX());
    final int centerZ = QuartPos.fromBlock(SpawnCenterHelper.getSpawnCenterZ());
    final int maxRadius = QuartPos.fromBlock(
      SpawnCenterHelper.getSpawnDistance()
    );

    BlockPos found = null;
    int count = 0;

    for (int radius = maxRadius; radius <= maxRadius; radius += step) {
      for (int dx = -radius; dx <= radius; dx += step) {
        for (int dz = -radius; dz <= radius; dz += step) {
          final int quartX = centerX + dz;
          final int quartZ = centerZ + dx;
          final BiomeExtension biome =
            ((BiomeSourceExtension) this).getBiomeExtensionNoRiver(
                quartX,
                quartZ
              );
          if (biome.isSpawnable()) {
            if (found == null || random.nextInt(count + 1) == 0) {
              found = new BlockPos(
                QuartPos.toBlock(quartX),
                0,
                QuartPos.toBlock(quartZ)
              );
            }
            count++;
          }
        }
      }
    }
    if (found == null) {
      TerraFirmaCraft.LOGGER.warn("Unable to find spawn biome!");
      return new BlockPos(
        SpawnCenterHelper.getSpawnCenterX(),
        0,
        SpawnCenterHelper.getSpawnCenterZ()
      );
    }
    return found;
  }
}
