package net.yazloysasha.tfcrealworld.mixin.world;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.dries007.tfc.world.ChunkHeightFiller;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.region.RegionPartition;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.river.RiverInfo;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.MapLakeWater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While a column is sampled, a lake biome's {@code isSalty()} can see the
 * column's block position and take its salinity from the map lake cells.
 * Rivers are looked for further away, for the wide rivers of the map.
 */
@Mixin(value = ChunkHeightFiller.class, remap = false)
public abstract class ChunkHeightFillerMixin {

  /**
   * Vanilla only looks for rivers within 50 blocks of a column, enough for
   * the banks of its widest river (24). The great rivers of the map are up to
   * 40 wide, and their banks reach twice that far from the middle.
   */
  @Unique
  private static final float RIVER_REACH_BLOCKS = 80f;

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

  /** At sea there is no river to carve a bed or to flow. */
  @Inject(method = "sampleRiverEdge", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$noRiverAtSea(
    RegionPartition.Point point,
    CallbackInfoReturnable<RiverInfo> cir
  ) {
    if (ContinentNoiseRegistry.isSeaAtBlock(blockX, blockZ)) {
      cir.setReturnValue(null);
    }
  }

  @ModifyConstant(
    method = "sampleRiverEdge",
    constant = @Constant(
      floatValue = (50f * 50f) /
        (Units.GRID_WIDTH_IN_BLOCK * Units.GRID_WIDTH_IN_BLOCK)
    )
  )
  private float tfcrealworld$riverReach(float vanillaReachInGridSquared) {
    return (
      (RIVER_REACH_BLOCKS * RIVER_REACH_BLOCKS) /
      (Units.GRID_WIDTH_IN_BLOCK * Units.GRID_WIDTH_IN_BLOCK)
    );
  }
}
