package net.yazloysasha.tfcrealworld.world.layer;

import java.util.function.IntPredicate;
import java.util.function.Predicate;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.AdjacentTransformLayer;
import net.dries007.tfc.world.layer.framework.AreaContext;

/**
 * Grows shore one cell inland via {@link TFCLayers#shoreFor(int)}.
 * Triggers only next to true ocean or an already-painted coastal shore biome
 * (not lakes — even if a lake biome flags {@code .shore()} for blend).
 * <p>
 * The inland biome is {@code shoreFor(center)}, the same choice vanilla uses
 * for that land.
 */
public enum WidenShoreInlandLayer implements AdjacentTransformLayer {
  INSTANCE;

  @Override
  public int apply(
    AreaContext context,
    int north,
    int east,
    int south,
    int west,
    int center
  ) {
    // TFC's hasShore does not leave the sea out.
    if (TFCLayers.isLake(center) || TFCLayers.isOcean(center)) {
      return center;
    }
    if (!TFCLayers.hasShore(center) || isShoreBiome(center)) {
      return center;
    }

    final Predicate<IntPredicate> matcher = p ->
      p.test(north) || p.test(east) || p.test(south) || p.test(west);
    if (
      !(matcher.test(TFCLayers::isOcean) ||
        matcher.test(WidenShoreInlandLayer::isShoreBiome))
    ) {
      return center;
    }

    return TFCLayers.shoreFor(center);
  }

  /**
   * Coastal shore already painted by ShoreLayer / prior widen passes.
   * Do NOT use BiomeExtension.isShore() alone: a lake biome may set .shore()
   * for height/blend but is not ocean coast — treating it as adjacency would
   * ring inland lakes with whatever {@code shoreFor} returns for that lake.
   */
  static boolean isShoreBiome(int value) {
    return (
      TFCLayers.getFromLayerId(value).isShore() && !TFCLayers.isLake(value)
    );
  }
}
