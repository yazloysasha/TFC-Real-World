package net.yazloysasha.tfcrealworld.world.region;

import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.LevelSeedRegistry;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.backend.Backends;
import net.yazloysasha.tfcrealworld.world.backend.WorldBackend;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.Volcanism;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import org.jetbrains.annotations.Nullable;

/**
 * What a generator's ChooseBiomes is told about the point it is choosing
 * for. The generator still picks every biome from its own pools; with
 * tectonics a pool is narrowed to the biomes that fit the map.
 */
public final class MapBiomeChoice {

  /** The region a thread is choosing biomes for. */
  private static final ThreadLocal<MapBiomeChoice> CURRENT =
    ThreadLocal.withInitial(MapBiomeChoice::new);

  private WorldBackend backend;

  @Nullable
  private TectonicsMap tectonics;

  @Nullable
  private MapHotspotLayout volcanoesAsMountains;

  private Region.Point point;
  private int x;
  private int z;

  private MapBiomeChoice() {}

  public static MapBiomeChoice enter(RegionGenerator.Context context) {
    final MapBiomeChoice choice = CURRENT.get();
    choice.backend = Backends.current();
    choice.tectonics = TectonicsRegistry.get(context.generator());
    choice.volcanoesAsMountains =
      choice.tectonics == null || choice.backend.hasHotspots()
        ? null
        : TectonicsRegistry.hotspotLayout();
    return choice;
  }

  public static MapBiomeChoice current() {
    return CURRENT.get();
  }

  /** Volcanic cones are moved onto the volcanic biomes the map gave. */
  public static void leave(RegionGenerator.Context context) {
    final MapBiomeChoice choice = CURRENT.get();
    toGeneratorClimate(context.region, choice.backend);
    if (choice.tectonics != null) {
      choice.backend.alignCenteredFeatures(
        context.region,
        LevelSeedRegistry.get(context.generator()),
        choice.tectonics
      );
    }
    choice.point = null;
  }

  /**
   * The climate maps are drawn in TFC 4's temperature scale, which a
   * generator's biome choice is written in too where it reads temperature.
   * Once the biomes are chosen, the temperatures go into the scale of the
   * generator's own climate.
   */
  private static void toGeneratorClimate(Region region, WorldBackend backend) {
    if (!TFCRealWorldConfig.CLIMATE_FROM_MAP.get()) {
      return;
    }
    for (final Region.Point point : region.data()) {
      if (point != null) {
        point.temperature = backend.climateTemperature(point.temperature);
      }
    }
  }

  public void at(Region.Point point, int x, int z) {
    this.point = point;
    this.x = x;
    this.z = z;
  }

  public boolean fromMap() {
    return tectonics != null;
  }

  /** Whether the map has volcanism, of a boundary or a hotspot, here. */
  public boolean volcanicGround() {
    if (tectonics == null) {
      return false;
    }
    if (tectonics.classAtGrid(x, z).volcanism() != Volcanism.NONE) {
      return true;
    }
    return (
      volcanoesAsMountains != null && volcanoesAsMountains.ageAtGrid(x, z) > 0
    );
  }

  /** The sea where coral reefs stand, per the tectonics map. */
  public boolean atolls() {
    return tectonics != null && tectonics.classAtGrid(x, z).atolls();
  }

  /**
   * The generator's seeded pick among the choices that fit the first rule
   * that leaves any, or among all of them if no rule does or the map has no
   * tectonics.
   */
  public int pick(
    long rngSeed,
    int areaSeed,
    int[] choices,
    IntPredicate... rules
  ) {
    int[] fitting = choices;
    if (tectonics != null) {
      for (final IntPredicate rule : rules) {
        final int[] kept = IntStream.of(choices).filter(rule).toArray();
        if (kept.length > 0) {
          fitting = kept;
          break;
        }
      }
    }
    return fitting[Math.floorMod(rngSeed ^ (long) areaSeed, fitting.length)];
  }

  /**
   * An island takes any of the generator's island biomes. With tectonics
   * the map decides: islands here are the low ones (a mountainous island is
   * ordinary land with mountain relief), volcanic where the map has
   * volcanism and not where it has none.
   */
  public int pickIsland(long rngSeed, int areaSeed, int[] choices) {
    final boolean volcanic = volcanicGround() || backend.volcanic(point);
    final IntPredicate volcanism = biome ->
      backend.isVolcanicBiome(biome) == volcanic;
    return pick(
      rngSeed,
      areaSeed,
      choices,
      volcanism.and(biome -> !backend.isMountains(biome)),
      volcanism
    );
  }

  /**
   * TFC 3 keeps its volcanic mountains in the coastal pool only and picks
   * them at random. With tectonics a range is volcanic where the map has
   * volcanism, from whichever pool has the biome, and coastal where the
   * sea reaches into it.
   */
  public int pickMountain(
    long rngSeed,
    int areaSeed,
    int[] choices,
    int[] inland,
    int[] coastal
  ) {
    if (tectonics == null) {
      return pick(rngSeed, areaSeed, choices);
    }
    if (!volcanicGround()) {
      // Mountain relief of the map is a young range: the generator's old
      // mountains, plateaus and highlands come from its highland relief.
      final IntPredicate quiet = biome -> !backend.isVolcanicBiome(biome);
      return pick(
        rngSeed,
        areaSeed,
        choices,
        quiet.and(
          biome -> backend.isMountains(biome) && biome != backend.oldMountains()
        ),
        quiet
      );
    }
    final boolean oceanic = point.coastalMountain();
    final IntPredicate volcanic = backend::isVolcanicBiome;
    return pick(
      rngSeed,
      areaSeed,
      IntStream.concat(IntStream.of(inland), IntStream.of(coastal)).toArray(),
      volcanic.and(biome -> backend.biome(biome).isSalty() == oceanic),
      volcanic
    );
  }

  /**
   * TFC 3 fills its shallower seafloor with open sea, reefs and deep sea
   * at random, and only away from a plate boundary. With tectonics the
   * map's reef seafloor is its reefs, wherever the boundaries run.
   */
  public int pickMidDepthOcean(long rngSeed, int areaSeed, int[] choices) {
    return pick(
      rngSeed,
      areaSeed,
      choices,
      biome -> biome == backend.oceanReef()
    );
  }

  public byte oceanDistanceToEdge(Region.Point point) {
    return tectonics != null ? Byte.MAX_VALUE : point.distanceToEdge;
  }

  /**
   * "Meets the sea" for an ice sheet: with tectonics only where the ice
   * sheet edge layer finds the ocean next to it, never by distance.
   */
  public byte iceSheetDistanceToOcean(Region.Point point) {
    return tectonics != null ? Byte.MAX_VALUE : point.distanceToOcean;
  }

  /**
   * Collisional mountains are oceanic where the sea reaches into the range
   * (the {@code coast} property), as every other mountain.
   */
  public byte collisionalDistanceToOcean(Region.Point point) {
    if (tectonics == null) {
      return point.distanceToOcean;
    }
    return point.coastalMountain() ? 0 : Byte.MAX_VALUE;
  }

  /**
   * The generator builds atolls in warm sea far enough from land, which on
   * a real map is nearly all of the tropical ocean. With tectonics the
   * distance is replaced by the {@code atolls} property.
   */
  public byte atollDistanceToLand(byte distanceToLand) {
    if (tectonics == null) {
      return distanceToLand;
    }
    return atolls() ? Byte.MAX_VALUE : 0;
  }
}
