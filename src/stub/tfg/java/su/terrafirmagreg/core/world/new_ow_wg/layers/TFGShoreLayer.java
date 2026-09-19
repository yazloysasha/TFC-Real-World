package su.terrafirmagreg.core.world.new_ow_wg.layers;

import net.dries007.tfc.world.layer.framework.AreaContext;

public enum TFGShoreLayer {
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
