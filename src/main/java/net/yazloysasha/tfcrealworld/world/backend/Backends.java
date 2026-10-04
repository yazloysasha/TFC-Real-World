package net.yazloysasha.tfcrealworld.world.backend;

import net.yazloysasha.tfcrealworld.compat.TfeCompat;
import net.yazloysasha.tfcrealworld.compat.TfgCompat;

/**
 * The world generator in use. TerraFirmaGreg decides per world whether its
 * backport or TFC 3 generates, so the answer is asked for each time.
 */
public final class Backends {

  private static final WorldBackend TFC3 = new Tfc3Backend();

  private Backends() {}

  public static WorldBackend current() {
    if (TfgCompat.useTfgOverworldPipeline()) {
      return Tfg.INSTANCE;
    }
    if (TfeCompat.useTfeOverworldPipeline()) {
      return Tfe.INSTANCE;
    }
    return TFC3;
  }

  /** Loaded only with TerraFirmaGreg present. */
  private static final class Tfg {

    static final WorldBackend INSTANCE = new TfgBackend();
  }

  /** Loaded only with TerraFirmaEarth present. */
  private static final class Tfe {

    static final WorldBackend INSTANCE = new TfeBackend();
  }
}
