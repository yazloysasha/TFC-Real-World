package net.yazloysasha.tfcrealworld.world.surface.tfe;

import com.newterraearth.tfe.world.surface.ShorelineSurfaceBuilder;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.yazloysasha.tfcrealworld.mixin.world.surface.tfe.TfeShorelineSurfaceBuilderAccessor;

/**
 * TerraFirmaEarth lays sea ice and icebergs where the warmest month stays
 * near freezing, but only in the biomes of its shore and ocean surface
 * builder: a lake, a river or a salt marsh in the same cold stays open
 * water, and an iceberg ends in a straight wall at the biome's border. This
 * builds a biome's own surface and then runs the generator's routine on the
 * open water at sea level, in every biome alike, with fresh ice on fresh
 * water.
 */
public final class TfeFrozenWaterSurfaceBuilder implements SurfaceBuilder {

  private final SurfaceBuilder surface;
  private final TfeShorelineSurfaceBuilderAccessor ice;

  public TfeFrozenWaterSurfaceBuilder(SurfaceBuilder surface, long seed) {
    this.surface = surface;
    this.ice =
      (TfeShorelineSurfaceBuilderAccessor) ShorelineSurfaceBuilder.OCEAN.apply(
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
    final boolean fresh = context.getBlockState(surfaceY).is(Blocks.WATER);
    if (
      !fresh && !context.getBlockState(surfaceY).is(TFCBlocks.SALT_WATER.get())
    ) {
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
    ice.tfcrealworld$invokeFrozenOceanExtension(
      context,
      startY,
      endY,
      context.chunk().getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z),
      seaLevel
    );
    if (fresh && context.getBlockState(surfaceY).is(TFCBlocks.SEA_ICE.get())) {
      context.setBlockState(surfaceY, Blocks.ICE.defaultBlockState());
    }
  }
}
