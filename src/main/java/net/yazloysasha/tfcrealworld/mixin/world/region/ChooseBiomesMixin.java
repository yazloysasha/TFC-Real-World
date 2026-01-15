package net.yazloysasha.tfcrealworld.mixin.world.region;

import static net.dries007.tfc.world.layer.TFCLayers.DEEP_OCEAN;
import static net.dries007.tfc.world.layer.TFCLayers.DEEP_OCEAN_TRENCH;
import static net.dries007.tfc.world.layer.TFCLayers.OCEAN;

import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChooseBiomes.class)
public class ChooseBiomesMixin {

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$overrideOceanBiomes(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    final ChooseBiomesAccessor accessor = (ChooseBiomesAccessor) this;

    final Region region = context.region;
    final Area blobArea = context.generator().biomeArea.get();
    final long rngSeed = context.random.nextLong();
    final int[] midDepthOceanBiomes =
      accessor.tfcrealworld$getMidDepthOceanBiomes();

    for (final var point : region.points()) {
      if (!point.land() && !point.island() && !point.mountain()) {
        final int areaSeed = blobArea.get(point.x, point.z);

        if (point.baseOceanDepth < 7) {
          point.biome = OCEAN;
        } else if (point.baseOceanDepth > 9) {
          point.biome = DEEP_OCEAN_TRENCH;
        } else if (point.baseOceanDepth >= 5) {
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
