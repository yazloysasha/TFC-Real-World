package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.region.AnnotateBiomeAltitude;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.mixin.world.region.RegionPointAccessor;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.registry.RainVarianceRegistry;
import net.yazloysasha.tfcrealworld.util.registry.RiversRegistry;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.backend.Backends;
import net.yazloysasha.tfcrealworld.world.backend.WorldBackend;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise.ContinentBand;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainVarianceNoise;
import net.yazloysasha.tfcrealworld.world.region.calculator.OceanDistanceCalculator;
import net.yazloysasha.tfcrealworld.world.region.calculator.WestCoastDistanceCalculator;
import net.yazloysasha.tfcrealworld.world.river.MapRivers;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.LandRelief;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.Volcanism;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import org.jetbrains.annotations.Nullable;

/**
 * The region tasks that read the maps, written once for every world
 * generator: {@code continent.png} decides land and water,
 * {@code tectonics.png} what kind of place it is, and the generator's own
 * ChooseBiomes turns the point fields written here into biomes. A task
 * answers whether it took the generator's place.
 */
public final class MapRegionTasks {

  private static final short FLAG_LAND = 0b1;
  private static final short FLAG_ISLAND = 0b10;
  private static final short FLAG_MOUNTAIN = 0b10000;
  private static final short FLAG_COASTAL_MOUNTAIN = 0b100000;
  /** Age of an ancient hotspot, the only one that raises no land. */
  private static final byte ANCIENT_HOTSPOT = 4;

  private MapRegionTasks() {}

  /**
   * Land, islands and ocean depths of the points the generator has made.
   * A lake of the map is land here: a region cell is far larger than the
   * map's lakes, so the biome layer floods exactly the map's lake pixels.
   */
  public static boolean continents(RegionGenerator.Context context) {
    final RegionGenerator generator = context.generator();
    final PNGContinentNoise continent = continentOf(generator);
    if (continent == null) {
      return false;
    }
    final WorldBackend backend = Backends.current();
    final TectonicsMap tectonics = TectonicsRegistry.get(generator);
    final MapHotspotLayout volcanoes = volcanoesAsMountains(backend, tectonics);
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
      if (tectonics == null) {
        applyProcedural(point, band, continent, generator, backend, x, z);
        continue;
      }
      final TectonicClass place = tectonics.classAtGrid(x, z);
      backend.setDivergence(point, place.boundary().divergence());
      applyTectonics(point, band, place, continent, backend, x, z);
      if (volcanoes != null && raisesLand(volcanoes.ageAtGrid(x, z))) {
        point.setLand();
      }
    }
    return true;
  }

  /**
   * continent.png says what is an island (island band, also islets inside
   * ocean cells). tectonics.png says what kind: a mountainous island is
   * plain land with mountain relief; a low one is one of the generator's
   * islands, on a volcanic arc its volcanic island chain where it has one.
   */
  private static void applyTectonics(
    Region.Point point,
    ContinentBand band,
    TectonicClass place,
    PNGContinentNoise continent,
    WorldBackend backend,
    int x,
    int z
  ) {
    final Volcanism volcanism = place.volcanism();
    final boolean arc =
      volcanism == Volcanism.ARC && backend.hasTectonicFields();
    final boolean island =
      place.land() != LandRelief.MOUNTAIN &&
      (band == ContinentBand.ISLAND ||
        (band == ContinentBand.OCEAN && continent.anyIslandInCell(x, z)));
    if (island && arc) {
      // The volcanic island chain, trimmed to continent.png land later.
      backend.setOceanDepth(point, 1);
      backend.setVolcanic(point);
      backend.setBarrierIsland(point);
      return;
    }
    if (island) {
      point.setLand();
      point.setIsland();
      setVolcanism(point, volcanism, backend);
      return;
    }
    if (band == ContinentBand.OCEAN) {
      if (arc) {
        backend.setOceanDepth(point, 1);
        backend.setVolcanic(point);
      } else {
        backend.setOceanDepth(point, place.water().oceanDepth());
      }
      return;
    }
    point.setLand();
    setVolcanism(point, volcanism, backend);
  }

  private static void setVolcanism(
    Region.Point point,
    Volcanism volcanism,
    WorldBackend backend
  ) {
    if (volcanism != Volcanism.NONE) {
      backend.setVolcanic(point);
    }
  }

  /**
   * Continent map without tectonics: the generator's ocean depth buckets
   * over the binary continent continuum and its own plate boundaries.
   */
  private static void applyProcedural(
    Region.Point point,
    ContinentBand band,
    PNGContinentNoise continent,
    RegionGenerator generator,
    WorldBackend backend,
    int x,
    int z
  ) {
    if (band != ContinentBand.OCEAN) {
      point.setLand();
      if (band == ContinentBand.ISLAND) {
        point.setIsland();
      }
      return;
    }
    if (!backend.hasTectonicFields()) {
      return;
    }
    final float divergence = backend.divergence(point);
    final double riftSeas =
      point.distanceToEdge <= 5 && divergence > 0
        ? (5 - point.distanceToEdge) * -0.12
        : 0;
    final double depth =
      (continent.noise(x, z) + riftSeas) *
      backend.continentFactor(generator, x, z);
    if (divergence > 0 && point.distanceToEdge < 2) {
      backend.setOceanDepth(point, 3);
    } else if (depth > TfcContinentNoiseThresholds.CONTINENTAL_SHELF) {
      backend.setOceanDepth(point, 2);
    } else if (
      depth > TfcContinentNoiseThresholds.TRENCH_CONTINENT && divergence < 0
    ) {
      backend.setOceanDepth(point, 5);
    } else if (point.distanceToEdge < 2 && !(divergence < 0 && depth > 2)) {
      backend.setOceanDepth(point, 3);
    } else {
      backend.setOceanDepth(point, 4);
    }
  }

  /**
   * With volcanoes from the map, hotspots stand where the tectonics map has
   * them. As in the generator, every hotspot but an ancient one raises land.
   */
  public static boolean hotspots(RegionGenerator.Context context) {
    if (!TectonicsRegistry.isActive(context.generator())) {
      return false;
    }
    final MapHotspotLayout layout = TectonicsRegistry.hotspotLayout();
    if (layout == null) {
      return false;
    }
    final WorldBackend backend = Backends.current();
    final Region region = context.region;
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      final byte age = layout.ageAtGrid(
        RegionCoords.gridX(region, index),
        RegionCoords.gridZ(region, index)
      );
      if (age > 0) {
        backend.setHotSpotAge(point, age);
        if (raisesLand(age)) {
          point.setLand();
        }
      }
    }
    return true;
  }

  /** Distance to the nearest plate boundary zone of the tectonics map. */
  public static boolean distanceToCellEdge(RegionGenerator.Context context) {
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

  public static boolean distanceToOcean(RegionGenerator.Context context) {
    if (continentOf(context.generator()) == null) {
      return false;
    }
    OceanDistanceCalculator.apply(context.region);
    return true;
  }

  public static boolean distanceToWestCoast(RegionGenerator.Context context) {
    if (continentOf(context.generator()) == null) {
      return false;
    }
    WestCoastDistanceCalculator.apply(context.region, Backends.current());
    return true;
  }

  /**
   * After the generator's own pass: land height from the tectonic relief,
   * and for a generator whose ocean depth is TFC 3's, the depth of the
   * seafloor there, which its pass has just measured from the coast.
   */
  public static void baseLandHeight(RegionGenerator.Context context) {
    final TectonicsMap tectonics = TectonicsRegistry.get(context.generator());
    if (tectonics == null) {
      return;
    }
    final WorldBackend backend = Backends.current();
    final Region region = context.region;
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      final TectonicClass place = tectonics.classAtGrid(
        RegionCoords.gridX(region, index),
        RegionCoords.gridZ(region, index)
      );
      if (point.land()) {
        point.baseLandHeight = place.land().baseLandHeight();
      } else if (!backend.hasTectonicFields()) {
        backend.setOceanDepth(point, place.water().oceanDepth());
      }
    }
  }

  /**
   * Land relief of the tectonics map as the generator's discrete biome
   * altitude and mountain flag, in place of its mountain ranges and
   * altitude spread: which land is mountainous is data. Coastal mountains
   * are the ranges the sea reaches into. Without hotspot biomes a young
   * hotspot is a mountain, which the map's volcanism then makes volcanic.
   */
  public static boolean biomeAltitude(RegionGenerator.Context context) {
    final TectonicsMap tectonics = TectonicsRegistry.get(context.generator());
    if (tectonics == null) {
      return false;
    }
    final MapHotspotLayout volcanoes = volcanoesAsMountains(
      Backends.current(),
      tectonics
    );
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
      final int x = RegionCoords.gridX(region, index);
      final int z = RegionCoords.gridZ(region, index);
      final TectonicClass place = tectonics.classAtGrid(x, z);
      final boolean volcano =
        volcanoes != null && raisesLand(volcanoes.ageAtGrid(x, z));
      final LandRelief relief = volcano ? LandRelief.MOUNTAIN : place.land();
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
   * seasonality included), so every point takes the map values as they are,
   * without the generator's ocean and cell-edge biases.
   */
  public static boolean climate(RegionGenerator.Context context) {
    if (!TFCRealWorldConfig.CLIMATE_FROM_MAP.get()) {
      return false;
    }
    // A generator without a west coast task measures it with the climate.
    distanceToWestCoast(context);
    final RegionGenerator generator = context.generator();
    final PNGRainVarianceNoise variance = RainVarianceRegistry.get(generator);
    final WorldBackend backend = Backends.current();
    final Region region = context.region;
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      final int x = RegionCoords.gridX(region, index);
      final int z = RegionCoords.gridZ(region, index);
      point.temperature = (float) generator.temperatureNoise.noise(x, z);
      point.rainfall = Mth.clamp(
        (float) generator.rainfallNoise.noise(x, z),
        0,
        500
      );
      if (variance != null) {
        backend.setRainfallVariance(
          point,
          PNGRainVarianceNoise.calendarVariance(variance.noise(x, z), z)
        );
      }
    }
    return true;
  }

  /**
   * A profile with {@code rivers.bin} gives each region the real rivers
   * that lie in it.
   */
  public static boolean rivers(RegionGenerator.Context context) {
    final MapRivers rivers = RiversRegistry.get(context.generator());
    if (rivers == null) {
      return false;
    }
    rivers.addTo(context.region, context.generator().seed());
    wetMapLakes(context);
    return true;
  }

  /**
   * The generator makes the cell of every lake it places a little wetter.
   * The lakes of the map are not placed by it, so they get the same here.
   */
  public static void wetMapLakes(RegionGenerator.Context context) {
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

  /** The hotspots of the map for a generator with no hotspots of its own. */
  @Nullable
  private static MapHotspotLayout volcanoesAsMountains(
    WorldBackend backend,
    @Nullable TectonicsMap tectonics
  ) {
    return tectonics == null || backend.hasHotspots()
      ? null
      : TectonicsRegistry.hotspotLayout();
  }

  private static boolean raisesLand(byte hotSpotAge) {
    return hotSpotAge > 0 && hotSpotAge != ANCIENT_HOTSPOT;
  }

  private static void clear(Region.Point point, short flags) {
    final RegionPointAccessor accessor = (RegionPointAccessor) (Object) point;
    accessor.tfcrealworld$setFlags(
      (short) (accessor.tfcrealworld$getFlags() & ~flags)
    );
  }
}
