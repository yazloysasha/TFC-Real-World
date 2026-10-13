package net.yazloysasha.tfcrealworld.world.region;

import java.util.ArrayDeque;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.minecraft.world.level.levelgen.RandomSource;

/**
 * The region task being run. TFC 2 keeps its {@code RegionContext}
 * package-private, so the tasks see this instead.
 */
public final class RegionContext {

  /** A task may generate another region: the contexts nest. */
  private static final ThreadLocal<ArrayDeque<RegionContext>> RUNNING =
    ThreadLocal.withInitial(ArrayDeque::new);

  public final Region region;
  public final RandomSource random;
  private final RegionGenerator generator;

  private RegionContext(
    Region region,
    RandomSource random,
    RegionGenerator generator
  ) {
    this.region = region;
    this.random = random;
    this.generator = generator;
  }

  public RegionGenerator generator() {
    return generator;
  }

  public static RegionContext current() {
    return RUNNING.get().peek();
  }

  public static void enter(
    Region region,
    RandomSource random,
    RegionGenerator generator
  ) {
    RUNNING.get().push(new RegionContext(region, random, generator));
  }

  public static void leave() {
    RUNNING.get().pop();
  }
}
