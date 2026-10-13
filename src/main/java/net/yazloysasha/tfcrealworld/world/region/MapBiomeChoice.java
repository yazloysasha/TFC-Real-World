package net.yazloysasha.tfcrealworld.world.region;

import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.LevelSeedRegistry;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.Volcanism;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicPlaces;
import net.yazloysasha.tfcrealworld.world.volcano.CenteredFeatureAligner;
import org.jetbrains.annotations.Nullable;

/**
 * What TFC's ChooseBiomes is told about the point it is choosing for.
 * TFC still picks every biome from its own pools; with
 * tectonics a pool is narrowed to the biomes that fit the map.
 */
public final class MapBiomeChoice {

  /** The region a thread is choosing biomes for. */
  private static final ThreadLocal<MapBiomeChoice> CURRENT =
    ThreadLocal.withInitial(MapBiomeChoice::new);

  @Nullable
  private TectonicPlaces places;

  /** The answer of volcanicGround() for the point at hand, once asked. */
  @Nullable
  private Boolean volcanicGround;

  private Region.Point point;
  private int x;
  private int z;

  private MapBiomeChoice() {}

  public static MapBiomeChoice enter(RegionContext context) {
    final MapBiomeChoice choice = CURRENT.get();
    choice.places = TectonicPlaces.of(context.generator());
    return choice;
  }

  public static MapBiomeChoice current() {
    return CURRENT.get();
  }

  /** Volcanic cones are moved onto the volcanic biomes the map gave. */
  public static void leave(RegionContext context) {
    final MapBiomeChoice choice = CURRENT.get();
    toTfcClimate(context.region);
    if (choice.places != null) {
      // TFC stands every volcano in the cells of one noise.
      final Cellular2D cells = new Cellular2D(
        LevelSeedRegistry.get(context.generator())
      ).spread(0.009f);
      CenteredFeatureAligner.stamp(
        context.region,
        choice.places.map(),
        (x, z) -> {
          final Cellular2D.Cell cell = cells.cell(x, z);
          return new double[] { cell.x(), cell.y() };
        },
        biome -> isVolcanic(biome) && TFCLayers.isMountains(biome)
      );
    }
    choice.point = null;
  }

  /**
   * The climate maps are drawn in TFC 4's temperature scale, which the
   * biome choice is written in too where it reads temperature. Once the
   * biomes are chosen, the temperatures go into TFC 2's scale.
   */
  private static void toTfcClimate(Region region) {
    if (!TFCRealWorldConfig.CLIMATE_FROM_MAP.get()) {
      return;
    }
    for (final Region.Point point : region.data()) {
      if (point != null) {
        point.temperature = ClimateScale.temperature(point.temperature);
      }
    }
  }

  public void at(Region.Point point, int x, int z) {
    this.point = point;
    this.x = x;
    this.z = z;
    this.volcanicGround = null;
  }

  public boolean fromMap() {
    return places != null;
  }

  /** Whether the map has volcanism, of a boundary or a hotspot, here. */
  public boolean volcanicGround() {
    Boolean volcanic = volcanicGround;
    if (volcanic == null) {
      volcanic =
        places != null && places.at(x, z).volcanism() != Volcanism.NONE;
      volcanicGround = volcanic;
    }
    return volcanic;
  }

  /**
   * TFC's seeded pick among the choices that fit the first rule
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
    if (places != null) {
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
   * An island takes any of TFC's island biomes. With tectonics
   * the map decides: islands here are the low ones (a mountainous island is
   * ordinary land with mountain relief), volcanic where the map has
   * volcanism and not where it has none.
   */
  public int pickIsland(long rngSeed, int areaSeed, int[] choices) {
    final boolean volcanic = volcanicGround();
    final IntPredicate volcanism = biome -> isVolcanic(biome) == volcanic;
    return pick(
      rngSeed,
      areaSeed,
      choices,
      volcanism.and(biome -> !TFCLayers.isMountains(biome)),
      volcanism
    );
  }

  /**
   * TFC keeps its volcanic mountains in the coastal pool only and picks
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
    if (places == null) {
      return pick(rngSeed, areaSeed, choices);
    }
    if (!volcanicGround()) {
      // Mountain relief of the map is a young range: TFC's old mountains
      // and plateaus come from its highland relief.
      final IntPredicate quiet = biome -> !isVolcanic(biome);
      return pick(
        rngSeed,
        areaSeed,
        choices,
        quiet.and(
          biome ->
            TFCLayers.isMountains(biome) && biome != TFCLayers.OLD_MOUNTAINS
        ),
        quiet
      );
    }
    final boolean oceanic = point.coastalMountain();
    final IntPredicate volcanic = MapBiomeChoice::isVolcanic;
    return pick(
      rngSeed,
      areaSeed,
      IntStream.concat(IntStream.of(inland), IntStream.of(coastal)).toArray(),
      volcanic.and(
        biome -> TFCLayers.getFromLayerId(biome).isSalty() == oceanic
      ),
      volcanic
    );
  }

  /**
   * TFC keeps a volcanic biome among its land that is no mountain and
   * picks it at random (its canyons). With tectonics all
   * of the map's volcanic ground is such a biome, whatever the relief, and
   * no other ground is.
   */
  public int pickLand(
    long rngSeed,
    int areaSeed,
    int[] choices,
    int[][] pools
  ) {
    if (places == null) {
      return pick(rngSeed, areaSeed, choices);
    }
    final IntPredicate volcanic = MapBiomeChoice::isVolcanic;
    if (volcanicGround()) {
      final int[] cones = Stream.of(pools)
        .flatMapToInt(IntStream::of)
        .filter(volcanic)
        .distinct()
        .toArray();
      if (cones.length > 0) {
        return pick(rngSeed, areaSeed, cones);
      }
    }
    return pick(rngSeed, areaSeed, choices, volcanic.negate());
  }

  /**
   * Whether a biome TFC swaps in after its pick may stand here.
   */
  public boolean allows(int biome) {
    return (
      places == null ||
      volcanicGround() ||
      TFCLayers.isMountains(biome) ||
      !isVolcanic(biome)
    );
  }

  /**
   * TFC fills its shallower seafloor with open sea, reefs and deep sea
   * at random, and only away from a plate boundary. With tectonics the
   * map's reef seafloor is its reefs, wherever the boundaries run.
   */
  public int pickMidDepthOcean(long rngSeed, int areaSeed, int[] choices) {
    return pick(
      rngSeed,
      areaSeed,
      choices,
      biome -> biome == TFCLayers.OCEAN_REEF
    );
  }

  public byte oceanDistanceToEdge(Region.Point point) {
    return places != null ? Byte.MAX_VALUE : point.distanceToEdge;
  }

  /** A biome that builds volcanoes. */
  private static boolean isVolcanic(int biome) {
    return TFCLayers.getFromLayerId(biome).isVolcanic();
  }
}
