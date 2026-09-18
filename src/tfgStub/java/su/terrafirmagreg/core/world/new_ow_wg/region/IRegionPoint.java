package su.terrafirmagreg.core.world.new_ow_wg.region;

/**
 * Compile-only stub matching TFG's duck-typed Point extras. The real interface
 * is provided by Core-Modern at runtime and is not packaged in this jar.
 */
public interface IRegionPoint {
  void tfg$setDistanceToWestCoast(byte dist);

  byte tfg$getDistanceToWestCoast();

  void tfg$setIsSurfaceRockKarst(boolean isKarst);

  boolean tfg$getIsSurfaceRockKarst();

  void tfg$setHotSpotAge(byte age);

  byte tfg$getHotSpotAge();

  int tfg$getX();

  int tfg$getZ();

  int tfg$getIndex();

  void tfg$init(int x, int z, int index);
}
