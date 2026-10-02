package net.yazloysasha.tfcrealworld.world.surface;

import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.ShoreAndOceanSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.yazloysasha.tfcrealworld.mixin.world.surface.builder.ShoreAndOceanSurfaceBuilderAccessor;

/**
 * Vanilla covers the sea with ice where the warmest month stays near
 * freezing, but only in the biomes of its shore and ocean surface builder: a
 * lake, a river or a salt marsh in the same cold stays open water, cut out
 * of the ice along the biome's border. This builds a biome's own surface and
 * then lays vanilla's ice on the open water at sea level by vanilla's rule,
 * with fresh ice on fresh water.
 */
public final class FrozenWaterSurfaceBuilder implements SurfaceBuilder {

  /** Vanilla frozenOceanExtension: no ice where the warmest month is warmer. */
  private static final float MAX_ICE_TEMPERATURE = 2f;

  private final SurfaceBuilder surface;
  private final ShoreAndOceanSurfaceBuilderAccessor ice;

  public FrozenWaterSurfaceBuilder(SurfaceBuilder surface, Seed seed) {
    this.surface = surface;
    this.ice =
      (ShoreAndOceanSurfaceBuilderAccessor) ShoreAndOceanSurfaceBuilder.OCEAN.apply(
        seed
      );
  }

  @Override
  public void buildSurface(
    SurfaceBuilderContext context,
    int startY,
    int endY
  ) {
    surface.buildSurface(context, startY, endY);
    final int seaLevel = context.getSeaLevel();
    final int surfaceY = seaLevel - 1;
    final BlockState water = context.getBlockState(surfaceY);
    final boolean fresh = water.is(Blocks.WATER);
    if (!fresh && !water.is(TFCBlocks.SALT_WATER.get())) {
      return;
    }
    final int x = context.pos().getX();
    final int z = context.pos().getZ();
    // Water under a roof (a lake under an ice sheet or inside a mountain)
    // is out of the weather.
    if (
      context.chunk().getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) >
      surfaceY
    ) {
      return;
    }
    final OverworldClimateModel model = OverworldClimateModel.getIfPresent(
      context.level()
    );
    if (model == null) {
      return;
    }
    final float warmestMonth = model.getAverageMonthlyTemperature(
      z,
      seaLevel,
      context.averageTemperature(),
      1,
      true
    );
    if (warmestMonth > MAX_ICE_TEMPERATURE) {
      return;
    }
    ice.tfcrealworld$invokePlaceSeaIce(context, x, z, seaLevel, warmestMonth);
    final BlockState placed = context.getBlockState(surfaceY);
    if (fresh && placed.is(TFCBlocks.SEA_ICE.get())) {
      context.setBlockState(surfaceY, Blocks.ICE.defaultBlockState());
    }
  }
}
