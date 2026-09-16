package net.yazloysasha.tfcrealworld.mixin.world.region;

import static net.dries007.tfc.world.layer.TFCLayers.*;

import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChooseBiomes.class, remap = false)
public class ChooseBiomesMixin {

  @Unique
  private static final int[] shallowOceanBiomes = new int[] {
    OCEAN,
    OCEAN_REEF,
  };

  /**
   * Maps ETOPO-derived ocean depth (1–15) to TFC 4.2 ocean biomes, aligned with vanilla
   * discrete {@code oceanDepth} classes where possible.
   */
  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$overrideOceanBiomesFromAltitudeMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }

    final ChooseBiomesAccessor accessor = (ChooseBiomesAccessor) this;

    final Region region = context.region;
    final Area blobArea = context.generator().biomeArea.get();
    final long rngSeed = context.random.nextLong();

    for (final Region.Point point : region.points()) {
      if (point.land() || point.island() || point.mountain()) {
        continue;
      }
      if (point.hotSpotAge > 0 || point.barrierIsland()) {
        continue;
      }

      final int depth = Byte.toUnsignedInt(point.oceanDepth);
      final int areaSeed = blobArea.get(point.x, point.z);

      if (depth <= 1) {
        point.biome = OCEAN_REEF;
      } else if (depth == 2) {
        point.biome = point.temperature > 12 && point.distanceToLand > 4
          ? OCEAN_ATOLLS
          : OCEAN;
      } else if (depth == 3) {
        point.biome = OCEAN_RIDGE;
      } else if (depth >= 10) {
        point.biome = DEEP_OCEAN_TRENCH;
      } else if (depth >= 7) {
        point.biome = point.temperature > 12 && point.distanceToLand > 3
          ? DEEP_OCEAN_ATOLLS
          : DEEP_OCEAN;
      } else if (depth >= 4) {
        point.biome = DEEP_OCEAN;
      } else {
        point.biome = accessor.tfcrealworld$invokeRandomSeededFrom(
          rngSeed,
          areaSeed,
          shallowOceanBiomes
        );
      }
    }
  }
}
