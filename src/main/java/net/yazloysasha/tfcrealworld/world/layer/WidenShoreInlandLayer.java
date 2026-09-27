package net.yazloysasha.tfcrealworld.world.layer;

import static net.dries007.tfc.world.layer.TFCLayers.RIVER_VALLEY;

import java.util.function.IntPredicate;
import java.util.function.Predicate;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.AdjacentTransformLayer;
import net.dries007.tfc.world.layer.framework.AreaContext;

/**
 * Grows shore one cell inland via {@link TFCLayers#shoreFor(int)}.
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
    if (center == RIVER_VALLEY) {
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

  static boolean isShoreBiome(int value) {
    return TFCLayers.getFromLayerId(value).isShore();
  }
}
