package net.yazloysasha.tfcrealworld.mixin.world;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.dries007.tfc.world.ChunkNoiseFiller;
import net.dries007.tfc.world.biome.BiomeBlendType;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A column of the sea is no river. Vanilla names a column after the heaviest
 * land biome around it, so off a coast the sea is still that land, which has
 * rivers: a river that ends at the shore turns the sea before its mouth into
 * a river biome with flowing fresh water. Map rivers end in the water, and
 * here the sea stays the sea.
 */
@Mixin(value = ChunkNoiseFiller.class, remap = false)
public abstract class ChunkNoiseFillerMixin {

  /**
   * Share of ocean biomes around a column from which it is the sea: the
   * share from which vanilla cuts the ground down to the sea's edge.
   */
  @Unique
  private static final double SEA_OCEAN_SHARE = 0.25;

  @Unique
  private static final String HAS_RIVERS =
    "Lnet/dries007/tfc/world/biome/BiomeExtension;hasRivers()Z";

  /** Whether the column being filled is the sea. */
  @Unique
  private boolean tfcrealworld$sea;

  @Inject(method = "updateLocalCaches", at = @At("HEAD"))
  private void tfcrealworld$findSea(
    CallbackInfo ci,
    @Local(argsOnly = true) Object2DoubleMap<BiomeExtension> biomeWeights
  ) {
    tfcrealworld$sea = false;
    if (
      !TFCRealWorldConfig.CONTINENT_FROM_MAP.get() ||
      !TFCRealWorldConfig.RIVERS_FROM_MAP.get()
    ) {
      return;
    }
    double ocean = 0;
    for (final Object2DoubleMap.Entry<
      BiomeExtension
    > entry : biomeWeights.object2DoubleEntrySet()) {
      if (entry.getKey().biomeBlendType() == BiomeBlendType.OCEAN) {
        ocean += entry.getDoubleValue();
      }
    }
    tfcrealworld$sea = ocean >= SEA_OCEAN_SHARE;
  }

  @Redirect(
    method = "updateLocalCaches",
    at = @At(value = "INVOKE", target = HAS_RIVERS)
  )
  private boolean tfcrealworld$noRiverBiomeAtSea(BiomeExtension biome) {
    return !tfcrealworld$sea && biome.hasRivers();
  }

  @Redirect(
    method = "fillColumn",
    at = @At(value = "INVOKE", target = HAS_RIVERS)
  )
  private boolean tfcrealworld$noRiverFlowAtSea(BiomeExtension biome) {
    return !tfcrealworld$sea && biome.hasRivers();
  }
}
