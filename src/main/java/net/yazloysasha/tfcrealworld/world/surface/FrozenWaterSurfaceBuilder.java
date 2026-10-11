package net.yazloysasha.tfcrealworld.world.surface;

import java.util.function.LongFunction;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * TerraFirmaEarth and TerraFirmaGreg lay sea ice and icebergs where the
 * warmest month stays near freezing, but only in the biomes of their shore
 * and ocean surface builder: a lake, a river or a salt marsh in the same cold
 * stays open water, and an iceberg ends in a straight wall at the biome's
 * border. This builds a biome's own surface and then runs the generator's
 * routine on the open water at sea level, in every biome alike, with fresh
 * ice on fresh water.
 */
public final class FrozenWaterSurfaceBuilder implements SurfaceBuilder {

  /** The generator's sea ice and iceberg routine for one column. */
  @FunctionalInterface
  public interface Ice {
    void freeze(
      SurfaceBuilderContext context,
      int startY,
      int endY,
      int oceanFloorY,
      int seaLevel
    );
  }

  /**
   * The generator's routine, made once per world seed. River surfaces ask
   * a biome for its surface builder in every column, and the routine's
   * iceberg noises are too costly to make that often.
   */
  public static final class IceBySeed {

    private record Made(long seed, Ice ice) {}

    private final LongFunction<Ice> factory;

    private volatile Made made;

    public IceBySeed(LongFunction<Ice> factory) {
      this.factory = factory;
    }

    public Ice get(long seed) {
      Made last = made;
      if (last == null || last.seed() != seed) {
        last = new Made(seed, factory.apply(seed));
        made = last;
      }
      return last.ice();
    }
  }

  private final SurfaceBuilder surface;
  private final Ice ice;

  public FrozenWaterSurfaceBuilder(SurfaceBuilder surface, Ice ice) {
    this.surface = surface;
    this.ice = ice;
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
    ice.freeze(
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
