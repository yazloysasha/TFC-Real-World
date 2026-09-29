package net.yazloysasha.tfcrealworld.mixin.world;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.dries007.tfc.world.ChunkHeightFiller;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.yazloysasha.tfcrealworld.world.MapLakeWater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While a column is sampled, a lake biome's {@code isSalty()} can see the
 * column's block position and take its salinity from the map lake cells.
 */
@Mixin(value = ChunkHeightFiller.class, remap = false)
public abstract class ChunkHeightFillerMixin {

  @Shadow
  protected int blockX;

  @Shadow
  protected int blockZ;

  @Inject(method = "sampleColumnHeightAndBiome", at = @At("HEAD"))
  private void tfcrealworld$enterLakeColumn(
    Object2DoubleMap<BiomeExtension> biomeWeights,
    boolean useCache,
    CallbackInfoReturnable<Double> cir
  ) {
    MapLakeWater.enterColumn(blockX, blockZ);
  }

  @Inject(method = "sampleColumnHeightAndBiome", at = @At("RETURN"))
  private void tfcrealworld$leaveLakeColumn(
    Object2DoubleMap<BiomeExtension> biomeWeights,
    boolean useCache,
    CallbackInfoReturnable<Double> cir
  ) {
    MapLakeWater.leaveColumn();
  }
}
