package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.Region;
import org.jetbrains.annotations.Nullable;

public final class RiverRainfallExclusion {

  public static final float NO_RIVERS_RAINFALL_THRESHOLD_MM = 75f;

  private RiverRainfallExclusion() {}

  public static boolean excludes(@Nullable Region.Point point) {
    return (
      point != null &&
      point.land() &&
      point.rainfall < NO_RIVERS_RAINFALL_THRESHOLD_MM
    );
  }
}
