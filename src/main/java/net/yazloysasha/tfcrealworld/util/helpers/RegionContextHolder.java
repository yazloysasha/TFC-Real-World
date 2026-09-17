package net.yazloysasha.tfcrealworld.util.helpers;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.minecraft.world.level.levelgen.RandomSource;
import org.jetbrains.annotations.Nullable;

public final class RegionContextHolder {

  private static final ThreadLocal<Region> REGION = new ThreadLocal<>();
  private static final ThreadLocal<RegionGenerator> GENERATOR =
    new ThreadLocal<>();
  private static final ThreadLocal<RandomSource> RANDOM = new ThreadLocal<>();

  private RegionContextHolder() {}

  public static void set(
    Region region,
    RegionGenerator generator,
    RandomSource random
  ) {
    REGION.set(region);
    GENERATOR.set(generator);
    RANDOM.set(random);
  }

  @Nullable
  public static Region getRegion() {
    return REGION.get();
  }

  @Nullable
  public static RegionGenerator getGenerator() {
    return GENERATOR.get();
  }

  @Nullable
  public static RandomSource getRandom() {
    return RANDOM.get();
  }

  public static void clear() {
    REGION.remove();
    GENERATOR.remove();
    RANDOM.remove();
  }
}
