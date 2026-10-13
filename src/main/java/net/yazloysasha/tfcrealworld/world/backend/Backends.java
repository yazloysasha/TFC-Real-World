package net.yazloysasha.tfcrealworld.world.backend;

/** The world generator in use. */
public final class Backends {

  private static final WorldBackend TFC2 = new Tfc2Backend();

  private Backends() {}

  public static WorldBackend current() {
    return TFC2;
  }
}
