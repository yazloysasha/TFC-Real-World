package net.yazloysasha.tfcrealworld.world.layer;

import java.util.function.IntPredicate;
import java.util.function.Predicate;
import net.dries007.tfc.world.biome.BiomeBlendType;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.framework.AdjacentTransformLayer;
import net.dries007.tfc.world.layer.framework.AreaContext;
import net.yazloysasha.tfcrealworld.world.backend.WorldBackend;

/**
 * Grows shore one cell inland via the generator's {@code shoreFor}.
 * Triggers only next to true ocean or an already-painted coastal shore biome
 * (not lakes — even if a lake biome flags {@code .shore()} for blend).
 * <p>
 * The inland biome is {@code shoreFor(center)}, the same choice vanilla uses
 * for that land. A result that blends as ocean would be pulled under sea
 * level, so it is handed to its MoreShores layer, which already decides
 * what stands on the waterline beside that shore. Every other result is kept.
 */
public final class WidenShoreInlandLayer implements AdjacentTransformLayer {

  private final WorldBackend backend;

  public WidenShoreInlandLayer(WorldBackend backend) {
    this.backend = backend;
  }

  @Override
  public int apply(
    AreaContext context,
    int north,
    int east,
    int south,
    int west,
    int center
  ) {
    if (center == backend.riverValley() || backend.isLake(center)) {
      return center;
    }
    if (!backend.hasShore(center) || isShoreBiome(center)) {
      return center;
    }

    final Predicate<IntPredicate> matcher = p ->
      p.test(north) || p.test(east) || p.test(south) || p.test(west);
    if (!(matcher.test(backend::isOcean) || matcher.test(this::isShoreBiome))) {
      return center;
    }

    return inlandShore(context, backend.shoreFor(center));
  }

  /**
   * {@code shoreFor} as-is, unless it blends as ocean. Then the same
   * its MoreShores layer rule that rewrites the waterline.
   */
  private int inlandShore(AreaContext context, int shore) {
    if (!blendsAsOcean(shore)) {
      return shore;
    }
    return backend.moreShores(
      context,
      shore,
      backend.ocean(),
      shore,
      shore,
      shore
    );
  }

  private boolean blendsAsOcean(int layerId) {
    final BiomeExtension extension = backend.biome(layerId);
    return (
      extension.isShore() && extension.biomeBlendType() == BiomeBlendType.OCEAN
    );
  }

  /**
   * Coastal shore already painted by ShoreAndRiver / prior widen passes.
   * Do NOT use BiomeExtension.isShore() alone: MELTWATER_LAKE (and a few other
   * non-coast biomes) set .shore() for height/blend but are not ocean coast —
   * treating them as adjacency would ring inland lakes with whatever
   * {@code shoreFor} returns for that lake.
   */
  private boolean isShoreBiome(int value) {
    return (backend.biome(value).isShore() && !backend.isLake(value));
  }
}
