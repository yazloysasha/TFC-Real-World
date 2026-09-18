package su.terrafirmagreg.core.world.new_ow_wg.layers;

import net.dries007.tfc.world.layer.framework.AreaContext;

public enum TFGIceSheetEdgeLayer {
  INSTANCE;

  public int apply(
    AreaContext context,
    int north,
    int east,
    int south,
    int west,
    int center
  ) {
    return center;
  }
}
