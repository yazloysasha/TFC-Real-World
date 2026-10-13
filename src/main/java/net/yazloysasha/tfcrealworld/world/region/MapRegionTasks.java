package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.AnnotateBiomeAltitude;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.mixin.world.region.RegionGeneratorAccessor;
import net.yazloysasha.tfcrealworld.mixin.world.region.RegionPointAccessor;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.registry.LevelSeedRegistry;
import net.yazloysasha.tfcrealworld.util.registry.RiversRegistry;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise.ContinentBand;
import net.yazloysasha.tfcrealworld.world.region.calculator.OceanDistanceCalculator;
import net.yazloysasha.tfcrealworld.world.river.MapRivers;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.LandRelief;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicPlaces;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import org.jetbrains.annotations.Nullable;

/**
 * The region tasks that read the maps: {@code continent.png} decides land
 * and water, {@code tectonics.png} what kind of place it is, and TFC's own
 * ChooseBiomes turns the point fields written here into biomes. A task
 * answers whether it took TFC's place.
 */
public final class MapRegionTasks {

  private static final short FLAG_LAND = 0b1;
  private static final short FLAG_ISLAND = 0b10;
  private static final short FLAG_MOUNTAIN = 0b10000;
  private static final short FLAG_COASTAL_MOUNTAIN = 0b100000;
  /** TFC base ocean depths its biome choice reads as each depth class. */
  private static final byte[] BASE_OCEAN_DEPTH = { 0, 3, 1, 1, 6, 10 };

  private MapRegionTasks() {}

  /**
   * Land, islands and ocean depths of the points the generator has made.
   * A lake of the map is land here: a region cell is far larger than the
   * map's lakes, so the biome layer floods exactly the map's lake pixels.
   */
  public static boolean continents(RegionContext context) {
    final RegionGenerator generator = context.generator();
    final PNGContinentNoise continent = continentOf(generator);
    if (continent == null) {
      return false;
    }
    final TectonicPlaces places = TectonicPlaces.of(generator);
    final Region region = context.region;
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      final int x = RegionCoords.gridX(region, index);
      final int z = RegionCoords.gridZ(region, index);
      clear(point, (short) (FLAG_LAND | FLAG_ISLAND));
      // A region point covers [x, x + 1): sample the centre of its cell.
      final ContinentBand band = continent.bandAtGridHard(x + 0.5, z + 0.5);
      if (places == null) {
        applyProcedural(point, band);
        continue;
      }
      applyTectonics(
        point,
        places.risesFromTheSea(x, z) ? ContinentBand.LAND : band,
        places.at(x, z),
        continent,
        x,
        z
      );
    }
    return true;
  }

  /**
   * continent.png says what is an island (island band, also islets inside
   * ocean cells). tectonics.png says what kind: a mountainous island is
   * plain land with mountain relief; a low one is one of TFC's islands.
   */
  private static void applyTectonics(
    Region.Point point,
    ContinentBand band,
    TectonicClass place,
    PNGContinentNoise continent,
    int x,
    int z
  ) {
    final boolean island =
      place.land() != LandRelief.MOUNTAIN &&
      (band == ContinentBand.ISLAND ||
        (band == ContinentBand.OCEAN && continent.anyIslandInCell(x, z)));
    if (island) {
      point.setLand();
      point.setIsland();
      return;
    }
    if (band == ContinentBand.OCEAN) {
      setOceanDepth(point, place.water().oceanDepth());
      return;
    }
    point.setLand();
  }

  /**
   * Continent map without tectonics: land and islands of the map, and
   * TFC's own ocean depths.
   */
  private static void applyProcedural(Region.Point point, ContinentBand band) {
    if (band != ContinentBand.OCEAN) {
      point.setLand();
      if (band == ContinentBand.ISLAND) {
        point.setIsland();
      }
    }
  }

  /** Distance to the nearest plate boundary zone of the tectonics map. */
  public static boolean distanceToCellEdge(RegionContext context) {
    final TectonicsMap tectonics = TectonicsRegistry.get(context.generator());
    if (tectonics == null) {
      return false;
    }
    final Region region = context.region;
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      if (data[index] != null) {
        data[index].distanceToEdge = tectonics.distanceToBoundaryAtGrid(
          RegionCoords.gridX(region, index),
          RegionCoords.gridZ(region, index)
        );
      }
    }
    return true;
  }

  public static boolean distanceToOcean(RegionContext context) {
    if (continentOf(context.generator()) == null) {
      return false;
    }
    OceanDistanceCalculator.apply(context.region);
    return true;
  }

  /**
   * After TFC's own pass: land height from the tectonic relief, and the
   * depth of the seafloor, which its pass has just measured from the coast.
   */
  public static void baseLandHeight(RegionContext context) {
    final TectonicPlaces places = TectonicPlaces.of(context.generator());
    if (places == null) {
      return;
    }
    final Region region = context.region;
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      final TectonicClass place = places.at(
        RegionCoords.gridX(region, index),
        RegionCoords.gridZ(region, index)
      );
      if (point.land()) {
        point.baseLandHeight = place.land().baseLandHeight();
      } else {
        setOceanDepth(point, place.water().oceanDepth());
      }
    }
  }

  /**
   * Land relief of the tectonics map as TFC's discrete biome
   * altitude and mountain flag, in place of its mountain ranges and
   * altitude spread: which land is mountainous is data. Coastal mountains
   * are the ranges the sea reaches into. A young hotspot is a mountain,
   * which the map's volcanism then makes volcanic.
   */
  public static boolean biomeAltitude(RegionContext context) {
    final TectonicPlaces places = TectonicPlaces.of(context.generator());
    if (places == null) {
      return false;
    }
    final Region region = context.region;
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      clear(point, (short) (FLAG_MOUNTAIN | FLAG_COASTAL_MOUNTAIN));
      if (!point.land()) {
        point.biomeAltitude = 0;
        continue;
      }
      final TectonicClass place = places.at(
        RegionCoords.gridX(region, index),
        RegionCoords.gridZ(region, index)
      );
      final LandRelief relief = place.land();
      point.biomeAltitude = (byte) (relief.discreteAltitude() *
        AnnotateBiomeAltitude.WIDTH);
      if (relief == LandRelief.MOUNTAIN) {
        point.setMountain();
        if (place.coast()) {
          point.setCoastalMountain();
        }
      }
    }
    return true;
  }

  /**
   * Climate maps hold the final climate (coasts, currents and rainfall
   * included), so every point takes the map values as they are,
   * without TFC's ocean and cell-edge biases.
   */
  public static boolean climate(RegionContext context) {
    if (!TFCRealWorldConfig.CLIMATE_FROM_MAP.get()) {
      return false;
    }
    final RegionGenerator generator = context.generator();
    final RegionGeneratorAccessor noises = (RegionGeneratorAccessor) generator;
    final Region region = context.region;
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      final int x = RegionCoords.gridX(region, index);
      final int z = RegionCoords.gridZ(region, index);
      point.temperature = noises.tfcrealworld$temperatureNoise().noise(x, z);
      point.rainfall = Mth.clamp(
        noises.tfcrealworld$rainfallNoise().noise(x, z),
        0,
        500
      );
    }
    return true;
  }

  /**
   * A profile with {@code rivers.bin} gives each region the real rivers
   * that lie in it.
   */
  public static boolean rivers(RegionContext context) {
    final MapRivers rivers = RiversRegistry.get(context.generator());
    if (rivers == null) {
      return false;
    }
    rivers.addTo(context.region, LevelSeedRegistry.get(context.generator()));
    wetMapLakes(context);
    return true;
  }

  /**
   * TFC makes the cell of every lake it places a little wetter.
   * The lakes of the map are not placed by it, so they get the same here.
   */
  public static void wetMapLakes(RegionContext context) {
    final PNGContinentNoise continent = continentOf(context.generator());
    if (continent == null) {
      return;
    }
    final Region region = context.region;
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (
        point != null &&
        continent.isLakeAtGridHard(
          RegionCoords.gridX(region, index) + 0.5,
          RegionCoords.gridZ(region, index) + 0.5
        )
      ) {
        point.rainfall +=
          MapRivers.RAINFALL_SHARE * (MapRivers.MAX_RAINFALL - point.rainfall);
      }
    }
  }

  /** With lakes from the map its lake pixels are the lakes. */
  public static boolean lakesFromMap() {
    return (
      TFCRealWorldConfig.CONTINENT_FROM_MAP.get() &&
      TFCRealWorldConfig.LAKES_FROM_MAP.get()
    );
  }

  @Nullable
  private static PNGContinentNoise continentOf(RegionGenerator generator) {
    return TFCRealWorldConfig.CONTINENT_FROM_MAP.get()
      ? ContinentNoiseRegistry.get(generator)
      : null;
  }

  /**
   * Ocean depth in TFC 4 classes: 1 shallows of an arc or reef, 2 shelf,
   * 3 ridge, 4 deep ocean, 5 trench.
   */
  private static void setOceanDepth(Region.Point point, int depth) {
    point.baseOceanDepth = BASE_OCEAN_DEPTH[depth];
  }

  private static void clear(Region.Point point, short flags) {
    final RegionPointAccessor accessor = (RegionPointAccessor) (Object) point;
    accessor.tfcrealworld$setFlags(
      (short) (accessor.tfcrealworld$getFlags() & ~flags)
    );
  }
}
