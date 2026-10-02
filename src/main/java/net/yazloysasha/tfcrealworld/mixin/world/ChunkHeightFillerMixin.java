package net.yazloysasha.tfcrealworld.mixin.world;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Map;
import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.ChunkHeightFiller;
import net.dries007.tfc.world.biome.BiomeBlendType;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.region.Units;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.MapLakeWater;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While a column is sampled, a lake biome's {@code isSalty()} can see the
 * column's block position and take its salinity from the map lake cells.
 * Rivers are looked for further away, for the wide rivers of the map, and lake
 * forms of different height are not averaged into each other: vanilla gives a
 * map lake the form of the land under each cell, a subglacial lake (its bed
 * the ice surface) next to a meltwater one, and the average lifts the
 * meltwater out of the water. The lakes of a column share the lowest bed.
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

  /**
   * Share of the lowest lake among the lakes of a column from which the
   * others start to sink to its bed, and the share at which they are on it.
   * Below it the column keeps the vanilla average: the higher lake stays up.
   */
  @Unique
  private static final double LOW_LAKE_SHARE_NONE = 0.05;

  @Unique
  private static final double LOW_LAKE_SHARE_WHOLE = 0.25;

  @Shadow
  @Final
  protected Map<BiomeExtension, BiomeNoiseSampler> biomeNoiseSamplers;

  /** How far the lakes of the column sink to the lowest bed, 0 to 1. */
  @Unique
  private double tfcrealworld$sink;

  /** The lake with the lowest bed in the column being sampled. */
  @Unique
  @Nullable
  private BiomeExtension tfcrealworld$lake;

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

  @ModifyVariable(
    method = "sampleColumnHeightAndBiome",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/ChunkHeightFiller;computeInitialRiverWeights(Lit/unimi/dsi/fastutil/objects/Object2DoubleMap;)V"
    ),
    ordinal = 0
  )
  private double tfcrealworld$lakeBasin(
    double height,
    Object2DoubleMap<BiomeExtension> biomeWeights
  ) {
    tfcrealworld$lake = null;
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return height;
    }
    double lakes = 0;
    double lowest = Double.MAX_VALUE;
    for (final Object2DoubleMap.Entry<
      BiomeExtension
    > entry : biomeWeights.object2DoubleEntrySet()) {
      if (entry.getKey().biomeBlendType() == BiomeBlendType.LAKE) {
        lakes += entry.getDoubleValue();
        final double bed = biomeNoiseSamplers.get(entry.getKey()).height();
        if (bed < lowest) {
          lowest = bed;
          tfcrealworld$lake = entry.getKey();
        }
      }
    }
    if (tfcrealworld$lake == null) {
      return height;
    }
    tfcrealworld$sink = Mth.clampedMap(
      biomeWeights.getDouble(tfcrealworld$lake) / lakes,
      LOW_LAKE_SHARE_NONE,
      LOW_LAKE_SHARE_WHOLE,
      0,
      1
    );
    for (final Object2DoubleMap.Entry<
      BiomeExtension
    > entry : biomeWeights.object2DoubleEntrySet()) {
      if (entry.getKey().biomeBlendType() == BiomeBlendType.LAKE) {
        height -=
          tfcrealworld$sink *
          entry.getDoubleValue() *
          (biomeNoiseSamplers.get(entry.getKey()).height() - lowest);
      }
    }
    return height;
  }

  /** A lake column sunk to the lowest bed is that lake's. */
  @ModifyVariable(
    method = "sampleColumnHeightAndBiome",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/ChunkHeightFiller;sampleRiverInfo(Z)Lnet/dries007/tfc/world/river/RiverInfo;"
    ),
    ordinal = 0
  )
  private BiomeExtension tfcrealworld$lakeBasinBiome(BiomeExtension biomeAt) {
    return (
        tfcrealworld$lake != null &&
        tfcrealworld$sink > 0.5 &&
        biomeAt.biomeBlendType() == BiomeBlendType.LAKE
      )
      ? tfcrealworld$lake
      : biomeAt;
  }

  @Inject(method = "sampleColumnHeightAndBiome", at = @At("RETURN"))
  private void tfcrealworld$leaveLakeColumn(
    Object2DoubleMap<BiomeExtension> biomeWeights,
    boolean useCache,
    CallbackInfoReturnable<Double> cir
  ) {
    MapLakeWater.leaveColumn();
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
