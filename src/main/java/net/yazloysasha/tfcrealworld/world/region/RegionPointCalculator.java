package net.yazloysasha.tfcrealworld.world.region;

import java.util.function.Consumer;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;

/**
 * Base class for region point calculators.
 */
public abstract class RegionPointCalculator {

  public abstract void calculate(Region region, RegionGenerator generator);

  protected void forEachPoint(
    Region region,
    Consumer<Region.Point> pointConsumer
  ) {
    for (final var point : region.points()) {
      if (point != null) {
        pointConsumer.accept(point);
      }
    }
  }
}
