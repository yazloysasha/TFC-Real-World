package net.yazloysasha.tfcrealworld.world.layer;

import static net.dries007.tfc.world.layer.TFCLayers.RIVER_VALLEY;

import java.util.function.IntPredicate;
import java.util.function.Predicate;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.AdjacentTransformLayer;
import net.dries007.tfc.world.layer.framework.AreaContext;

/**
 * Grows shore one cell inland via {@link TFCLayers#shoreFor(int)}.
 * Triggers only next to true ocean or an already-painted coastal shore biome
 * (not lakes — even if a lake biome flags {@code .shore()} for blend).
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
    if (center == RIVER_VALLEY || TFCLayers.isLake(center)) {
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
   * Coastal shore already painted by ShoreAndRiver / prior widen passes.
   * Do NOT use BiomeExtension.isShore() alone: MELTWATER_LAKE (and a few other
   * non-coast biomes) set .shore() for height/blend but are not ocean coast —
   * treating them as adjacency would ring inland lakes with TIDAL_FLATS /
   * ICE_SHEET_SHORE via shoreFor.
   */
  static boolean isShoreBiome(int value) {
    return (
      TFCLayers.getFromLayerId(value).isShore() && !TFCLayers.isLake(value)
    );
  }
}
