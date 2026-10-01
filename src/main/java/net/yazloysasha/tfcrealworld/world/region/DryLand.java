package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.Region;
import org.jetbrains.annotations.Nullable;

/**
 * Land too dry to carry rivers. Map climates have real deserts, where vanilla
 * would still start and route rivers; these rainfall limits keep them out.
 */
public final class DryLand {

  /** Annual rainfall below which no river starts, ends in a lake or marks a cell. */
  public static final float RIVER_SOURCE_MIN_RAINFALL = 100f;

  /** Annual rainfall below which a river course may not pass. */
  public static final float RIVER_COURSE_MIN_RAINFALL = 75f;

  private DryLand() {}

  public static boolean isDrierThan(
    @Nullable Region.Point point,
    float rainfall
  ) {
    return point != null && point.land() && point.rainfall < rainfall;
  }
}
