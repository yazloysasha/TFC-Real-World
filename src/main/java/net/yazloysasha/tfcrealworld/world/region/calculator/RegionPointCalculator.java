package net.yazloysasha.tfcrealworld.world.region.calculator;

import java.util.function.Consumer;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.jetbrains.annotations.Nullable;

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

  protected boolean isContinentFromMapEnabled() {
    return TFCRealWorldConfig.CONTINENT_FROM_MAP.get();
  }

  protected <T> boolean validateCache(@Nullable T cache) {
    return cache != null;
  }
}
