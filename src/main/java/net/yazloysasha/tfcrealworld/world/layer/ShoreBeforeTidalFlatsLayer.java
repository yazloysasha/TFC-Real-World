package net.yazloysasha.tfcrealworld.world.layer;

import java.util.function.IntPredicate;
import java.util.function.Predicate;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.AdjacentTransformLayer;
import net.dries007.tfc.world.layer.framework.AreaContext;

/**
 * TFC 4's MoreShores rule for the shores TFC 3 has: a cell on the waterline
 * that touches tidal flats is a shore. It is asked twice, as in TFC 4:
 * for each cell a shore grows inland onto, which leaves the flats on the
 * waterline alone, and once more over the finished shore, which leaves
 * them where no two lie side by side.
 */
public enum ShoreBeforeTidalFlatsLayer implements AdjacentTransformLayer {
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
    final Predicate<IntPredicate> matcher = p ->
      p.test(north) || p.test(east) || p.test(south) || p.test(west);
    return (
        matcher.test(TFCLayers::isOcean) &&
        matcher.test(biome -> biome == TFCLayers.TIDAL_FLATS)
      )
      ? TFCLayers.SHORE
      : center;
  }
}
