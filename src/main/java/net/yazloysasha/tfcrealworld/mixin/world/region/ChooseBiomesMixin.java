package net.yazloysasha.tfcrealworld.mixin.world.region;

import static net.dries007.tfc.world.layer.TFCLayers.*;

import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChooseBiomes.class, remap = false)
public class ChooseBiomesMixin {

  @Unique
  private static final int[] midDepthOceanBiomes = new int[] {
    OCEAN,
    OCEAN_REEF,
  };

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$overrideOceanBiomes(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    final ChooseBiomesAccessor accessor = (ChooseBiomesAccessor) this;

    final Region region = context.region;
    final Area blobArea = context.generator().biomeArea.get();
    final long rngSeed = context.random.nextLong();

    final int minX = region.minX();
    final int minZ = region.minZ();
    final int sizeX = region.sizeX();

    for (int index = 0; index < region.data().length; index++) {
      final var point = region.data()[index];
      if (point == null) continue;

      final int localX = index % sizeX;
      final int localZ = index / sizeX;
      final int gridX = minX + localX;
      final int gridZ = minZ + localZ;

      if (!point.land() && !point.island() && !point.mountain()) {
        final int areaSeed = blobArea.get(gridX, gridZ);

        if (point.baseOceanDepth < 4) {
          point.biome = OCEAN;
        } else if (point.baseOceanDepth > 9) {
          point.biome = DEEP_OCEAN_TRENCH;
        } else if (point.baseOceanDepth > 6) {
          point.biome = DEEP_OCEAN;
        } else {
          point.biome = accessor.tfcrealworld$invokeRandomSeededFrom(
            rngSeed,
            areaSeed,
            midDepthOceanBiomes
          );
        }
      }
    }
  }
}
