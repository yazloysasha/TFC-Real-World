package net.yazloysasha.tfcrealworld.mixin.world.chunkdata;

import com.llamalad7.mixinextras.sugar.Local;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.RegionChunkDataGenerator;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainVarianceNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Vanilla scales a river's groundwater by its width up to its own widest
 * river. Map rivers can be wider, and count as the widest.
 * <p>
 * Vanilla blends a chunk's rainfall variance from the four region points
 * around it. With climate maps a point south of the equator holds the
 * opposite sign of the same climate north of it, so a chunk next to the
 * equator reads the points across it with the sign of its own hemisphere.
 */
@Mixin(value = RegionChunkDataGenerator.class, remap = false)
public class RegionChunkDataGeneratorMixin {

  @Unique
  private static final String VARIANCE_LAYER =
    "Lnet/dries007/tfc/world/chunkdata/LerpFloatLayer;<init>(FFFF)V";

  @ModifyArg(
    method = "generate",
    at = @At(value = "INVOKE", target = VARIANCE_LAYER, ordinal = 1),
    index = 1
  )
  private float tfcrealworld$variance01(
    float variance,
    @Local(argsOnly = true) ChunkData data
  ) {
    return tfcrealworld$inChunkHemisphere(variance, data);
  }

  @ModifyArg(
    method = "generate",
    at = @At(value = "INVOKE", target = VARIANCE_LAYER, ordinal = 1),
    index = 3
  )
  private float tfcrealworld$variance11(
    float variance,
    @Local(argsOnly = true) ChunkData data
  ) {
    return tfcrealworld$inChunkHemisphere(variance, data);
  }

  /**
   * The variance of the region point one cell south of the chunk's own, with
   * the sign of the hemisphere of the chunk's own point.
   */
  @Unique
  private static float tfcrealworld$inChunkHemisphere(
    float variance,
    ChunkData data
  ) {
    if (!TFCRealWorldConfig.CLIMATE_FROM_MAP.get()) {
      return variance;
    }
    final int gridZ = Units.blockToGrid(data.getPos().getMinBlockZ());
    return (
        PNGRainVarianceNoise.isNorth(gridZ) ==
        PNGRainVarianceNoise.isNorth(gridZ + 1)
      )
      ? variance
      : -variance;
  }

  @ModifyVariable(
    method = "adjustGroundwaterNearRiver",
    at = @At("HEAD"),
    ordinal = 1,
    argsOnly = true
  )
  private float tfcrealworld$capWidthInfluence(float widthInfluence) {
    return Math.min(widthInfluence, 1f);
  }
}
