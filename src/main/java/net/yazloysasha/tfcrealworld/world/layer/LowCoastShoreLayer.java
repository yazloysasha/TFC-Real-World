package net.yazloysasha.tfcrealworld.world.layer;

import java.util.function.IntPredicate;
import java.util.function.Predicate;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.AdjacentTransformLayer;
import net.dries007.tfc.world.layer.framework.AreaContext;
import net.yazloysasha.tfcrealworld.world.backend.Tfc3Backend;

/**
 * TFC 3's shore layer with the shore TFC 4 gives each land: tidal flats on
 * the coast of plains and badlands, a shore elsewhere. TFC 3 itself lays
 * tidal flats on the sea beside every shore, which on a map coastline would
 * fill straits and bays.
 */
public enum LowCoastShoreLayer implements AdjacentTransformLayer {
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
        !TFCLayers.isOcean(center) &&
        TFCLayers.hasShore(center) &&
        matcher.test(TFCLayers::isOcean)
      )
      ? Tfc3Backend.shoreOf(center)
      : center;
  }
}
