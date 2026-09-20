package com.newterraearth.tfe.world.layer;

import net.dries007.tfc.world.layer.framework.AreaContext;

public enum NTERiverShoreLayer {
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
