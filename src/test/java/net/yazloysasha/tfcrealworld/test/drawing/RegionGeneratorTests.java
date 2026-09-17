package net.yazloysasha.tfcrealworld.test.drawing;

import static net.dries007.tfc.world.layer.TFCLayers.*;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.region.ChooseRocks;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.RegionGenerator.Task;
import net.dries007.tfc.world.region.RiverEdge;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.settings.Settings;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.test.TestSetup;
import net.yazloysasha.tfcrealworld.util.profile.MapProfile;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegionGeneratorTests implements TestSetup {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    RegionGeneratorTests.class
  );

  private static final double[][] CITIES_GEOGRAPHIC_COORDS = {
    { 12.4964, 41.9028 },
    { -77.0369, 38.9072 },
    { 38.7225, 14.1211 },
  };

  final DoubleFunction<Color> blue = Artist.Colors.linearGradient(
    new Color(50, 50, 150),
    new Color(100, 140, 255)
  );

  final DoubleFunction<Color> green = Artist.Colors.linearGradient(
    new Color(0, 100, 0),
    new Color(80, 200, 80)
  );

  final DoubleFunction<Color> teal = Artist.Colors.linearGradient(
    new Color(0, 150, 150),
    new Color(40, 250, 250)
  );

  final DoubleFunction<Color> temperature = Artist.Colors.multiLinearGradient(
    new Color(180, 20, 240),
    new Color(0, 180, 240),
    new Color(180, 180, 220),
    new Color(210, 210, 0),
    new Color(200, 120, 60),
    new Color(200, 40, 40)
  );

  @Test
  public void testRegionGenerator() {
    // Coordinates are given in grid scale, so 1 px = 128 blocks, 150 ~ 20km
    drawStitchedRegions(
      "",
      EnumSet.allOf(DrawnTask.class),
      RandomSupport.generateUniqueSeed(),
      0,
      0,
      312
    );
  }

  @SuppressWarnings("SameParameterValue")
  private void drawStitchedRegions(
    String name,
    Set<DrawnTask> tasksToDraw,
    long seed,
    int centerX,
    int centerZ,
    int radius
  ) {
    final Map<Task, List<DrawnTask>> taskParent = tasksToDraw
      .stream()
      .collect(Collectors.groupingBy(t -> t.root));

    final int taskIndex = tasksToDraw.size();
    final int[] taskOffset = new int[DrawnTask.values().length]; // DrawnTask.ordinal -> (Ascending) Index
    int index = -1;
    for (DrawnTask task : tasksToDraw) taskOffset[task.ordinal()] = ++index;

    final int size = radius * 2;
    final int[] taskData = new int[tasksToDraw.size() * radius * radius * 4]; // Color[(x + z * size) * taskIndex + taskOffset[task]]

    Arrays.fill(taskData, -1);

    final Settings settings = BuiltinWorldPreset.defaultSettings();
    final RegionGenerator generator = new RegionGenerator(
      settings,
      Seed.of(seed)
    );

    LOGGER.info("RegionGenerator artist seed: {}", seed);

    final Map<KoppenClimateClassification, Integer> koppenCounts =
      new EnumMap<>(KoppenClimateClassification.class);
    for (KoppenClimateClassification climate : KoppenClimateClassification.values()) koppenCounts.put(
      climate,
      0
    );

    final Map<Integer, Integer> biomeCounts = new HashMap<>();

    for (int dx = 0; dx < size; dx++) for (int dz = 0; dz < size; dz++) if (
      taskData[(dx + size * dz) * taskIndex] == -1
    ) generator.visualizeRegion(
      centerX - radius + dx,
      centerZ - radius + dz,
      (task, region) -> {
        for (DrawnTask drawnTask : taskParent.getOrDefault(
          task,
          List.of()
        )) for (Region.Point point : region.points()) {
          final int pointX = point.x - centerX + radius;
          final int pointZ = point.z - centerZ + radius;
          if (pointX >= 0 && pointX < size && pointZ >= 0 && pointZ < size) {
            taskData[(pointX + size * pointZ) * taskIndex +
              taskOffset[drawnTask.ordinal()]] = taskColor(
              drawnTask,
              region,
              point.x,
              point.z
            ).getRGB();
          }
        }
      }
    );

    collectFinalRegionStatistics(
      generator,
      seed,
      centerX,
      centerZ,
      size,
      koppenCounts,
      biomeCounts
    );

    for (DrawnTask task : tasksToDraw) Draw.draw(
      taskName(name, task),
      size,
      size,
      (x, z) ->
        taskData[(x + size * z) * taskIndex + taskOffset[task.ordinal()]]
    );

    logKoppenStatistics(koppenCounts);
    logBiomeStatistics(biomeCounts);
  }

  private Artist.Pixel<Color> drawWithRivers(
    RegionGenerator generator,
    DrawnTask task
  ) {
    // Unused for now until I figure out a better way to hook it into drawing that doesn't explode memory usage
    return (xi, zi) -> {
      final int x = (int) xi, z = (int) zi;
      final float xf = (float) xi, zf = (float) zi;
      final Region region = generator.getOrCreateRegion(x, z);
      final Region.Point point = generator.getOrCreateRegionPoint(x, z);

      // Early exit - we don't draw rivers over oceans
      if (point.land() || point.shore()) {
        for (RiverEdge edge : generator
          .getOrCreatePartitionPoint(x, z)
          .rivers()) {
          if (edge.fractal().intersect(xf, zf, 0.1f)) { // Use a slightly larger distance than is typical, so we draw it more visibly
            return new Color(100, 210, 250);
          }
        }
      }
      return taskColor(task, region, x, z);
    };
  }

  private String taskName(String name, DrawnTask task) {
    return "region%s_%02d_%s".formatted(
        name,
        task.ordinal(),
        task.name().toLowerCase(Locale.ROOT)
      );
  }

  private Color taskColor(DrawnTask task, Region region, int x, int y) {
    final Region.Point point = region.at(x, y);
    assert point != null;
    return switch (task) {
      case ADD_CONTINENTS, FLOOD_FILL_SMALL_OCEANS, ADD_ISLANDS -> point.land()
        ? new Color(0, 130, 0)
        : oceanDepthColor(point);
      case ANNOTATE_DISTANCE_TO_CELL_EDGE -> blue.apply(
        point.distanceToEdge / 24f
      );
      case ANNOTATE_DISTANCE_TO_OCEAN_AND_DEEP_OCEAN -> point.land()
        ? green.apply(point.distanceToOcean / 20f)
        : point.oceanDepth == 2
          ? teal.apply(1 - point.distanceToDeepOcean / 20f)
          : cellColor(region);
      case ANNOTATE_BASE_LAND_HEIGHT -> continentColor(point);
      case ADD_MOUNTAINS -> mountainColor(point);
      case ANNOTATE_DISTANCE_TO_WEST_COAST -> point.land()
        ? green.apply(point.distanceToWestCoast / 100f)
        : cellColor(region);
      case ANNOTATE_BIOME_ALTITUDE -> point.land()
        ? green.apply(Mth.clampedMap(point.discreteBiomeAltitude(), 0, 3, 0, 1))
        : continentColor(point);
      case ANNOTATE_HOT_SPOT_AGE -> point.hotSpotAge > 0
        ? hotspot(point.hotSpotAge)
        : point.land()
          ? green.apply(
            Mth.clampedMap(point.discreteBiomeAltitude(), 0, 3, 0, 1)
          )
          : continentColor(point);
      case ANNOTATE_BOUNDARY_TYPES -> point.distanceToEdge < 2
        ? point.divergence > 0 ? Color.MAGENTA : Color.RED
        : point.land()
          ? green.apply(0.5 * point.divergence + 0.5)
          : point.divergence > 0 ? Color.BLUE : Color.ORANGE;
      case TEMPERATURE -> temperatureGradient(
        point,
        point.temperature,
        -25f,
        35f
      );
      case RAINFALL, RAINFALL_AFTER_RIVERS -> temperatureGradient(
        point,
        point.rainfall,
        0,
        500
      );
      case RAINFALL_VARIANCE -> oceanOutlineTemperatureGradient(
        point,
        point.rainfallVariance,
        -1,
        1
      );
      case KOPPEN, KOPPEN_AFTER_RIVERS -> point.land()
        ? koppenClimateColor(
          KoppenClimateClassification.classify(
            point.temperature,
            point.rainfall,
            point.rainfallVariance,
            isNorthernHemisphere(point.z)
          )
        )
        : continentColor(point);
      case CHOOSE_ROCKS -> {
        final double value = new Random(point.rock >> 2).nextDouble();
        yield switch (point.rock & 0b11) {
          case ChooseRocks.OCEAN -> blue.apply(value);
          case ChooseRocks.LAND -> green.apply(value);
          case ChooseRocks.VOLCANIC -> new Color(200, (int) (100 * value), 100);
          case ChooseRocks.UPLIFT -> new Color(180, (int) (180 * value), 200);
          default -> throw new RuntimeException("value: " + point.rock);
        };
      }
      case ANNOTATE_KARST_SURFACE -> {
        if (!point.land()) {
          yield blue.apply(point.isSurfaceRockKarst ? 0 : 30);
        } else if (point.isSurfaceRockKarst) {
          yield new Color(50, 200, 100);
        } else {
          yield new Color(200, 200, 10);
        }
      }
      case ADD_RIVERS_AND_LAKES -> {
        if (point.river()) yield new Color(100, 210, 250);
        if (point.shore()) yield new Color(240, 224, 120);
        if (point.lake()) yield new Color(150, 160, 255);
        yield continentColor(point);
      }
      case CHOOSE_BIOMES -> biomeColor(point.biome);
      case ANNOTATE_BIOMES_BY_HEIGHT -> heightBiomeColor(point.biome);
      case ANNOTATE_KARST_BIOMES -> karstBiomeColor(point.biome);
      case ANNOTATE_GLACIAL_BIOMES -> glaciatedBiomeColor(point.biome);
      case ANNOTATE_TECTONIC_BIOMES -> tectonicBiomeColor(point.biome);
      case KAOLINITE_CAN_SPAWN -> point.temperature > 18f &&
        point.rainfall > 300 &&
        point.land()
        ? point.biome == HIGHLANDS ||
          point.biome == PLATEAU ||
          point.biome == OLD_MOUNTAINS ||
          point.biome == ROLLING_HILLS ||
          point.biome == TOWER_KARST_HILLS ||
          point.biome == TOWER_KARST_HIGHLANDS ||
          point.biome == EXTREME_DOLINE_PLATEAU ||
          point.biome == EXTREME_DOLINE_MOUNTAINS ||
          point.biome == DOLINE_ROLLING_HILLS ||
          point.biome == DOLINE_HIGHLANDS ||
          point.biome == DOLINE_PLATEAU ||
          point.biome == CENOTE_ROLLING_HILLS ||
          point.biome == CENOTE_HIGHLANDS ||
          point.biome == CENOTE_PLATEAU ||
          point.biome == SHILIN_HIGHLANDS ||
          point.biome == SHILIN_PLATEAU ||
          point.biome == SHILIN_HILLS
          ? Color.MAGENTA
          : Color.PINK
        : continentColor(point);
      case PROJECTION_GRID -> {
        Color baseColor = taskColor(
          DrawnTask.ADD_RIVERS_AND_LAKES,
          region,
          x,
          y
        );
        if (isCityLocation(x, y)) {
          yield Color.WHITE;
        }
        yield switch (getGridLineType(x, y)) {
          case THIRTY_DEG -> Color.YELLOW;
          case TEN_DEG -> Color.BLACK;
          case NONE -> baseColor;
        };
      }
    };
  }

  private enum GridLineType {
    NONE,
    TEN_DEG,
    THIRTY_DEG,
  }

  private GridLineType getGridLineType(int gridX, int gridZ) {
    MapProfile profile = MapProfile.loadFromResources(
      TFCRealWorldConfig.DEFAULT_MAP_PROFILE
    );

    double classicX = gridX * Units.GRID_WIDTH_IN_BLOCK;
    double classicZ = gridZ * Units.GRID_WIDTH_IN_BLOCK;

    double[] geoCoords = ProjectionManager.classicToGeographic(
      classicX,
      classicZ,
      profile.horizontalScale(),
      profile.verticalScale(),
      profile.westEdgeLongitude(),
      profile.eastEdgeLongitude(),
      profile.southEdgeLatitude(),
      profile.northEdgeLatitude(),
      profile.mapProjection()
    );

    double longitude = geoCoords[0];
    double latitude = geoCoords[1];

    if (!Double.isFinite(longitude) || !Double.isFinite(latitude)) {
      return GridLineType.NONE;
    }

    double gridStep10 = 10.0;
    double gridStep30 = 30.0;

    double lonRemainder10 = Math.abs(
      ((longitude % gridStep10) + gridStep10) % gridStep10
    );
    double latRemainder10 = Math.abs(
      ((latitude % gridStep10) + gridStep10) % gridStep10
    );

    double lonRemainder30 = Math.abs(
      ((longitude % gridStep30) + gridStep30) % gridStep30
    );
    double latRemainder30 = Math.abs(
      ((latitude % gridStep30) + gridStep30) % gridStep30
    );

    double tolerance = 0.3;

    boolean onLongitude10 =
      lonRemainder10 < tolerance || lonRemainder10 > (gridStep10 - tolerance);
    boolean onLatitude10 =
      latRemainder10 < tolerance || latRemainder10 > (gridStep10 - tolerance);

    boolean onLongitude30 =
      lonRemainder30 < tolerance || lonRemainder30 > (gridStep30 - tolerance);
    boolean onLatitude30 =
      latRemainder30 < tolerance || latRemainder30 > (gridStep30 - tolerance);

    double lonNormalized = ((longitude % 360.0) + 360.0) % 360.0;
    double lonAbs = Math.abs(longitude);
    double latAbs = Math.abs(latitude);
    boolean onLongitudeCentral =
      lonAbs < tolerance || Math.abs(lonNormalized - 180.0) < tolerance;
    boolean onLatitudeCentral = latAbs < tolerance;

    if (
      onLongitudeCentral || onLatitudeCentral || onLongitude30 || onLatitude30
    ) {
      return GridLineType.THIRTY_DEG;
    }
    if (onLongitude10 || onLatitude10) {
      return GridLineType.TEN_DEG;
    }

    return GridLineType.NONE;
  }

  private boolean isCityLocation(int gridX, int gridZ) {
    MapProfile profile = MapProfile.loadFromResources(
      TFCRealWorldConfig.DEFAULT_MAP_PROFILE
    );

    double classicX = gridX * Units.GRID_WIDTH_IN_BLOCK;
    double classicZ = gridZ * Units.GRID_WIDTH_IN_BLOCK;

    double[] geoCoords = ProjectionManager.classicToGeographic(
      classicX,
      classicZ,
      profile.horizontalScale(),
      profile.verticalScale(),
      profile.westEdgeLongitude(),
      profile.eastEdgeLongitude(),
      profile.southEdgeLatitude(),
      profile.northEdgeLatitude(),
      profile.mapProjection()
    );

    double longitude = geoCoords[0];
    double latitude = geoCoords[1];

    if (!Double.isFinite(longitude) || !Double.isFinite(latitude)) {
      return false;
    }

    double cityRadius = 2;

    for (double[] cityCoords : CITIES_GEOGRAPHIC_COORDS) {
      double cityLon = cityCoords[0];
      double cityLat = cityCoords[1];
      double dist = Math.sqrt(
        Math.pow(longitude - cityLon, 2) + Math.pow(latitude - cityLat, 2)
      );
      if (dist < cityRadius) {
        return true;
      }
    }

    return false;
  }

  private boolean isNorthernHemisphere(int z) {
    final Settings settings = BuiltinWorldPreset.defaultSettings();

    final float adjustedZ =
      z - (settings.temperatureScale() / 2f / Units.GRID_WIDTH_IN_BLOCK);
    final float poleToPoleDistance =
      (2f * settings.temperatureScale()) / Units.GRID_WIDTH_IN_BLOCK;
    final float normalizedZ = Mth.positiveModulo(
      adjustedZ,
      (poleToPoleDistance * 2)
    );
    return normalizedZ > poleToPoleDistance;
  }

  /**
   * Köppen uses region climate after the full region pipeline. Biome counts use the same
   * resolution as in-game {@link net.dries007.tfc.world.biome.BiomeSourceExtension}:
   * zoomed biome layers plus river fractals ( {@code RIVER} is never stored on {@link Region.Point#biome}).
   */
  private void collectFinalRegionStatistics(
    RegionGenerator generator,
    long seed,
    int centerX,
    int centerZ,
    int size,
    Map<KoppenClimateClassification, Integer> koppenCounts,
    Map<Integer, Integer> biomeCounts
  ) {
    final AreaFactory biomeLayerFactory = TFCLayers.createRegionBiomeLayer(
      generator,
      Seed.of(seed)
    );
    final Area biomeLayer = biomeLayerFactory.get();
    final int radius = size >> 1;

    for (int dx = 0; dx < size; dx++) {
      for (int dz = 0; dz < size; dz++) {
        final int gridX = centerX - radius + dx;
        final int gridZ = centerZ - radius + dz;
        if (!MapTileGridBounds.isInsidePrimaryMapTile(gridX, gridZ)) {
          continue;
        }
        final Region.Point point = generator.getOrCreateRegionPoint(
          gridX,
          gridZ
        );
        if (point.land()) {
          final KoppenClimateClassification koppen =
            KoppenClimateClassification.classify(
              point.temperature,
              point.rainfall,
              point.rainfallVariance,
              isNorthernHemisphere(gridZ)
            );
          koppenCounts.put(koppen, koppenCounts.get(koppen) + 1);
        }
        final int biome = resolveWorldBiomeLayerId(
          generator,
          biomeLayer,
          gridX,
          gridZ
        );
        biomeCounts.put(biome, biomeCounts.getOrDefault(biome, 0) + 1);
      }
    }
  }

  /** Matches {@link net.dries007.tfc.world.biome.BiomeSourceExtension#getBiomeExtension(int, int)} at cell center. */
  private int resolveWorldBiomeLayerId(
    RegionGenerator generator,
    Area biomeLayer,
    int gridX,
    int gridZ
  ) {
    final int blockX =
      Units.gridToBlock(gridX) + (Units.GRID_WIDTH_IN_BLOCK >> 1);
    final int blockZ =
      Units.gridToBlock(gridZ) + (Units.GRID_WIDTH_IN_BLOCK >> 1);
    final int quartX = QuartPos.fromBlock(blockX);
    final int quartZ = QuartPos.fromBlock(blockZ);
    final int layerId = biomeLayer.get(quartX, quartZ);
    final BiomeExtension biome = TFCLayers.getFromLayerId(layerId);
    if (biome.hasRivers()) {
      final double exactGridX = Units.blockToGridExact(blockX);
      final double exactGridZ = Units.blockToGridExact(blockZ);
      for (RiverEdge edge : generator
        .getOrCreatePartitionPoint(gridX, gridZ)
        .rivers()) {
        if (edge.fractal().intersect(exactGridX, exactGridZ, 0.08f)) {
          return RIVER;
        }
      }
    }
    return layerId;
  }

  private void logKoppenStatistics(
    Map<KoppenClimateClassification, Integer> koppenCounts
  ) {
    LOGGER.info(
      "=== Koppen Climate Statistics (land, after full region generation) ==="
    );
    final int totalLandCells = koppenCounts
      .values()
      .stream()
      .mapToInt(Integer::intValue)
      .sum();
    LOGGER.info("Total land cells: {}", totalLandCells);
    LOGGER.info("Distribution by climate:");
    for (KoppenClimateClassification climate : KoppenClimateClassification.values()) {
      final int count = koppenCounts.get(climate);
      if (count == 0) continue;
      final double percentage = totalLandCells > 0
        ? ((count * 100.0) / totalLandCells)
        : 0.0;
      LOGGER.info(
        "  {}: {} cells ({})",
        climate,
        count,
        String.format(Locale.ROOT, "%.2f%%", percentage)
      );
    }
    LOGGER.info("====================================================");
  }

  private void logBiomeStatistics(Map<Integer, Integer> biomeCounts) {
    LOGGER.info(
      "=== Biome Statistics (biome layers + rivers, cell center) ==="
    );
    final int totalBiomeCells = biomeCounts
      .values()
      .stream()
      .mapToInt(Integer::intValue)
      .sum();
    LOGGER.info("Total cells: {}", totalBiomeCells);
    LOGGER.info("Distribution by biome:");
    biomeCounts
      .entrySet()
      .stream()
      .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
      .forEach(entry -> {
        final int biome = entry.getKey();
        final int count = entry.getValue();
        final double percentage = totalBiomeCells > 0
          ? ((count * 100.0) / totalBiomeCells)
          : 0.0;
        LOGGER.info(
          "  {}: {} cells ({})",
          getBiomeName(biome),
          count,
          String.format(Locale.ROOT, "%.2f%%", percentage)
        );
      });
    final int uniqueBiomesOnMap = biomeCounts.size();
    final int totalPossibleBiomes = getAllPossibleBiomes().size();
    LOGGER.info(
      "Unique biomes on map: {} out of {} total biomes",
      uniqueBiomesOnMap,
      totalPossibleBiomes
    );
    final Set<Integer> missingBiomes = new HashSet<>(getAllPossibleBiomes());
    missingBiomes.removeAll(biomeCounts.keySet());
    if (!missingBiomes.isEmpty()) {
      LOGGER.info("Missing biomes ({}):", missingBiomes.size());
      missingBiomes
        .stream()
        .sorted()
        .forEach(biome -> LOGGER.info("  {}", getBiomeName(biome)));
    }
    LOGGER.info("============================================");
  }

  private static Set<Integer> allPossibleBiomesCache;
  private static Map<Integer, String> biomeNameCache;

  private Set<Integer> getAllPossibleBiomes() {
    if (allPossibleBiomesCache != null) return allPossibleBiomesCache;

    final Set<Integer> biomes = new HashSet<>();
    final Map<Integer, String> names = new HashMap<>();
    try {
      for (final Field field : TFCLayers.class.getDeclaredFields()) {
        if (
          Modifier.isStatic(field.getModifiers()) &&
          Modifier.isFinal(field.getModifiers()) &&
          field.getType() == int.class
        ) {
          field.setAccessible(true);
          final int biomeId = field.getInt(null);
          biomes.add(biomeId);
          names.put(biomeId, field.getName());
        }
      }
    } catch (IllegalAccessException e) {
      LOGGER.error("Failed to get all biomes from TFCLayers", e);
      return Set.of();
    }
    allPossibleBiomesCache = Set.copyOf(biomes);
    biomeNameCache = Map.copyOf(names);
    return allPossibleBiomesCache;
  }

  private String getBiomeName(int biome) {
    if (
      biomeNameCache != null && biomeNameCache.containsKey(biome)
    ) return biomeNameCache.get(biome);

    try {
      for (final Field field : TFCLayers.class.getDeclaredFields()) {
        if (
          Modifier.isStatic(field.getModifiers()) &&
          Modifier.isFinal(field.getModifiers()) &&
          field.getType() == int.class
        ) {
          field.setAccessible(true);
          if (field.getInt(null) == biome) return field.getName();
        }
      }
    } catch (IllegalAccessException e) {
      LOGGER.debug("Failed to get biome name for ID {}", biome, e);
    }
    return "UNKNOWN_BIOME_" + biome;
  }

  private Color cellColor(Region region) {
    return blue.apply(0.5 + 0.5 * region.noise());
  }

  private Color continentColor(Region.Point point) {
    if (point.land()) return green.apply(point.baseLandHeight / 24f);
    return oceanDepthColor(point);
  }

  private Color oceanDepthColor(Region.Point point) {
    return switch (point.oceanDepth) {
      case 1 -> new Color(150, 160, 255);
      case 2 -> new Color(120, 120, 240);
      case 3 -> new Color(105, 105, 210);
      case 4 -> new Color(90, 90, 180);
      case 5 -> new Color(60, 60, 120);
      default -> new Color(255, 100, 100); // Error detection
    };
  }

  private Color mountainColor(Region.Point point) {
    if (point.mountain()) {
      return point.volcanic()
        ? point.coastalMountain()
          ? new Color(200, 90, 40)
          : new Color(240, 110, 50)
        : point.coastalMountain()
          ? new Color(140, 140, 140)
          : new Color(180, 180, 180);
    } else if (point.barrierIsland()) {
      return point.volcanic()
        ? new Color(200, 40, 40)
        : new Color(220, 140, 140);
    } else {
      return continentColor(point);
    }
  }

  private Color temperatureGradient(
    Region.Point point,
    float value,
    float min,
    float max
  ) {
    return (point.land() ? temperature : blue).apply(
        Mth.clampedMap(value, min, max, 0f, 0.999f)
      );
  }

  private Color oceanOutlineTemperatureGradient(
    Region.Point point,
    float value,
    float min,
    float max
  ) {
    return (point.shore() ? green : temperature).apply(
        Mth.clampedMap(value, min, max, 0f, 0.999f)
      );
  }

  // Default biome color scheme, Karst Biomes invisible
  private Color biomeColor(int biome) {
    if (biome == OCEAN_REEF) return new Color(150, 160, 255);
    if (biome == OCEAN || biome == OCEAN_ATOLLS) return new Color(
      120,
      120,
      240
    );
    if (biome == OCEAN_RIDGE) return new Color(105, 105, 210);
    if (biome == DEEP_OCEAN || biome == DEEP_OCEAN_ATOLLS) return new Color(
      90,
      90,
      180
    );
    if (biome == DEEP_OCEAN_TRENCH) return new Color(60, 60, 120);
    if (biome == LAKE) return new Color(30, 30, 255);
    if (
      biome == MOUNTAIN_LAKE ||
      biome == OCEANIC_MOUNTAIN_LAKE ||
      biome == OLD_MOUNTAIN_LAKE ||
      biome == VOLCANIC_MOUNTAIN_LAKE ||
      biome == PLATEAU_LAKE
    ) return new Color(20, 180, 255);
    if (biome == RIVER || biome == RIVER_VALLEY) return new Color(0, 200, 255);
    if (biome == GUANO_ISLAND) return new Color(170, 170, 170);
    if (biome == VOLCANIC_ISLAND) return new Color(120, 50, 70);
    if (biome == VOLCANIC_MOUNTAIN_ISLANDS) return new Color(150, 60, 90);

    if (biome == RIFT_VALLEY) return new Color(80, 0, 80);
    if (biome == RIFT_LAKE) return new Color(80, 0, 160);

    if (
      biome == OCEANIC_MOUNTAINS || biome == VOLCANIC_OCEANIC_MOUNTAINS
    ) return new Color(255, 0, 255);
    if (
      biome == CANYONS ||
      biome == TOWER_KARST_CANYONS ||
      biome == SHILIN_CANYONS ||
      biome == DOLINE_CANYONS ||
      biome == CENOTE_CANYONS
    ) return new Color(180, 60, 255);
    if (biome == LOW_CANYONS) return new Color(200, 110, 255);
    if (
      biome == LOWLANDS ||
      biome == TOWER_KARST_BAY ||
      biome == SALT_MARSH ||
      biome == TOWER_KARST_LAKE
    ) return new Color(220, 150, 230);

    if (biome == OCEANIC_VOLCANIC_ARC) return new Color(100, 10, 10);
    if (biome == COLLISIONAL_MOUNTAINS) return new Color(215, 20, 20);
    if (biome == MOUNTAINS || biome == VOLCANIC_MOUNTAINS) return new Color(
      255,
      50,
      50
    );
    if (
      biome == OLD_MOUNTAINS || biome == EXTREME_DOLINE_MOUNTAINS
    ) return new Color(240, 100, 100);
    if (
      biome == PLATEAU ||
      biome == PLATEAU_WIDE ||
      biome == EXTREME_DOLINE_PLATEAU ||
      biome == CENOTE_PLATEAU ||
      biome == DOLINE_PLATEAU ||
      biome == SHILIN_PLATEAU ||
      biome == BURREN_PLATEAU
    ) return new Color(190, 120, 120);

    if (
      biome == BADLANDS ||
      biome == BURREN_BADLANDS ||
      biome == BURREN_BADLANDS_TALL
    ) return new Color(205, 160, 50);
    if (biome == STAIR_STEP_CANYONS) return new Color(250, 190, 0);
    if (biome == HOODOOS) return new Color(230, 180, 0);
    if (biome == MESAS) return new Color(210, 170, 0);
    if (biome == BUTTES) return new Color(190, 160, 0);
    if (biome == WHORLED_CANYONS) return new Color(250, 160, 0);

    if (biome == ROCKY_PLATEAU) return new Color(180, 160, 110);
    if (biome == DUNE_SEA || biome == GRASSY_DUNES) return new Color(
      250,
      210,
      140
    );
    if (biome == SALT_FLATS) return new Color(190, 190, 190);
    if (biome == MUD_FLATS) return new Color(190, 120, 100);

    if (biome == SHORE || biome == TIDAL_FLATS) return new Color(230, 210, 130);

    if (
      biome == HIGHLANDS ||
      biome == SHILIN_HIGHLANDS ||
      biome == TOWER_KARST_HIGHLANDS ||
      biome == DOLINE_HIGHLANDS ||
      biome == CENOTE_HIGHLANDS
    ) return new Color(20, 80, 30);
    if (
      biome == ROLLING_HILLS ||
      biome == DOLINE_ROLLING_HILLS ||
      biome == CENOTE_ROLLING_HILLS
    ) return new Color(50, 100, 50);
    if (
      biome == HILLS ||
      biome == SHILIN_HILLS ||
      biome == TOWER_KARST_HILLS ||
      biome == DOLINE_HILLS ||
      biome == CENOTE_HILLS
    ) return new Color(80, 130, 80);
    if (
      biome == PLAINS ||
      biome == BURREN_PLAINS ||
      biome == TOWER_KARST_PLAINS ||
      biome == DOLINE_PLAINS ||
      biome == CENOTE_PLAINS ||
      biome == SHILIN_PLAINS
    ) return new Color(100, 200, 100);

    if (biome == ACTIVE_SHIELD_VOLCANO) return new Color(255, 85, 0);
    if (biome == DORMANT_SHIELD_VOLCANO) return new Color(255, 105, 0);
    if (biome == EXTINCT_SHIELD_VOLCANO) return new Color(255, 135, 0);
    if (
      biome == ANCIENT_SHIELD_VOLCANO || biome == SUNKEN_SHIELD_VOLCANO
    ) return new Color(255, 155, 0);

    if (biome == ICE_SHEET || biome == SUBGLACIAL_LAKE) return new Color(
      255,
      255,
      255
    );
    if (biome == ICE_SHEET_OCEANIC) return new Color(215, 215, 215);
    if (biome == ICE_SHEET_TUYAS) return new Color(235, 235, 235);
    if (
      biome == ICE_SHEET_MOUNTAINS ||
      biome == ICE_SHEET_MOUNTAINS_EDGE ||
      biome == ICE_SHEET_VOLCANIC_MOUNTAINS
    ) return new Color(255, 195, 195);
    if (
      biome == ICE_SHEET_OCEANIC_MOUNTAINS ||
      biome == ICE_SHEET_OCEANIC_MOUNTAINS_EDGE ||
      biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS
    ) return new Color(255, 195, 255);
    if (biome == ICE_SHEET_SHIELD_VOLCANO) return new Color(255, 215, 185);

    if (biome == ICE_SHEET_EDGE || biome == ICE_SHEET_SHORE) return new Color(
      165,
      165,
      165
    );

    if (
      biome == PATTERNED_GROUND ||
      biome == INVERTED_PATTERNED_GROUND ||
      biome == STONE_CIRCLES
    ) return new Color(135, 135, 135);
    if (biome == KNOB_AND_KETTLE) return new Color(115, 115, 115);
    if (biome == DRUMLINS || biome == BURREN_ROCHE_MOUTONEE) return new Color(
      135,
      165,
      135
    );
    if (biome == TUYAS) return new Color(115, 145, 115);
    if (
      biome == GLACIATED_MOUNTAINS || biome == GLACIATED_VOLCANIC_MOUNTAINS
    ) return new Color(255, 165, 165);
    if (
      biome == GLACIATED_OCEANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS
    ) return new Color(255, 165, 255);
    if (biome == GLACIATED_SHIELD_VOLCANO) return new Color(255, 185, 125);
    if (
      biome == GLACIALLY_CARVED_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_MOUNTAINS
    ) return new Color(255, 135, 135);
    if (
      biome == GLACIALLY_CARVED_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
    ) return new Color(255, 135, 255);

    return Color.BLACK;
  }

  // Only shows Karst biomes and water biomes, color coded by Karst Variety
  private Color karstBiomeColor(int biome) {
    if (biome == OCEAN_REEF || biome == OCEANIC_VOLCANIC_ARC) return new Color(
      150,
      160,
      255
    );
    if (biome == OCEAN || biome == OCEAN_ATOLLS) return new Color(
      120,
      120,
      240
    );
    if (biome == OCEAN_RIDGE) return new Color(105, 105, 210);
    if (biome == DEEP_OCEAN || biome == DEEP_OCEAN_ATOLLS) return new Color(
      90,
      90,
      180
    );
    if (biome == DEEP_OCEAN_TRENCH) return new Color(60, 60, 120);
    if (biome == RIVER || biome == RIVER_VALLEY) return new Color(0, 200, 255);

    if (biome == TOWER_KARST_BAY) return new Color(230, 120, 220);
    if (biome == TOWER_KARST_LAKE) return new Color(230, 100, 220);
    if (biome == TOWER_KARST_PLAINS) return new Color(230, 100, 100);
    if (biome == TOWER_KARST_CANYONS) return new Color(200, 80, 80);
    if (biome == TOWER_KARST_HILLS) return new Color(180, 60, 60);
    if (biome == TOWER_KARST_HIGHLANDS) return new Color(160, 40, 40);
    if (biome == EXTREME_DOLINE_PLATEAU) return new Color(140, 40, 90);
    if (biome == EXTREME_DOLINE_MOUNTAINS) return new Color(120, 20, 80);

    if (biome == SHILIN_PLAINS) return new Color(100, 250, 180);
    if (biome == SHILIN_CANYONS) return new Color(90, 220, 160);
    if (biome == SHILIN_HILLS) return new Color(80, 190, 140);
    if (biome == SHILIN_HIGHLANDS) return new Color(70, 170, 120);
    if (biome == SHILIN_PLATEAU) return new Color(60, 140, 100);

    if (biome == BURREN_PLAINS) return new Color(140, 190, 255);
    if (biome == BURREN_BADLANDS) return new Color(120, 160, 210);
    if (biome == BURREN_ROCHE_MOUTONEE) return new Color(120, 160, 180);
    if (biome == BURREN_BADLANDS_TALL) return new Color(100, 130, 180);
    if (biome == BURREN_PLATEAU) return new Color(80, 100, 150);

    if (biome == DOLINE_PLAINS) return new Color(255, 255, 170);
    if (biome == DOLINE_CANYONS) return new Color(220, 220, 150);
    if (biome == DOLINE_HILLS) return new Color(190, 190, 120);
    if (biome == DOLINE_ROLLING_HILLS) return new Color(160, 160, 90);
    if (biome == DOLINE_HIGHLANDS) return new Color(140, 140, 70);
    if (biome == DOLINE_PLATEAU) return new Color(120, 120, 50);

    if (biome == CENOTE_PLAINS) return new Color(255, 220, 40);
    if (biome == CENOTE_CANYONS) return new Color(220, 200, 30);
    if (biome == CENOTE_HILLS) return new Color(190, 170, 25);
    if (biome == CENOTE_ROLLING_HILLS) return new Color(170, 150, 20);
    if (biome == CENOTE_HIGHLANDS) return new Color(150, 130, 10);
    if (biome == CENOTE_PLATEAU) return new Color(140, 110, 0);

    return Color.BLACK;
  }

  // Shows only ice sheets, biomes effected by past ice sheets
  private Color glaciatedBiomeColor(int biome) {
    if (biome == OCEAN_REEF || biome == OCEANIC_VOLCANIC_ARC) return new Color(
      150,
      160,
      255
    );
    if (biome == OCEAN || biome == OCEAN_ATOLLS) return new Color(
      120,
      120,
      240
    );
    if (biome == OCEAN_RIDGE) return new Color(105, 105, 210);
    if (biome == DEEP_OCEAN || biome == DEEP_OCEAN_ATOLLS) return new Color(
      90,
      90,
      180
    );
    if (biome == DEEP_OCEAN_TRENCH) return new Color(60, 60, 120);
    if (biome == LAKE) return new Color(30, 30, 255);
    if (
      biome == MOUNTAIN_LAKE ||
      biome == OCEANIC_MOUNTAIN_LAKE ||
      biome == OLD_MOUNTAIN_LAKE ||
      biome == VOLCANIC_MOUNTAIN_LAKE ||
      biome == PLATEAU_LAKE
    ) return new Color(20, 180, 255);
    if (biome == RIVER || biome == RIVER_VALLEY) return new Color(0, 200, 255);

    if (biome == ICE_SHEET || biome == SUBGLACIAL_LAKE) return new Color(
      255,
      255,
      255
    );
    if (biome == ICE_SHEET_OCEANIC) return new Color(215, 215, 215);
    if (biome == ICE_SHEET_TUYAS) return new Color(235, 235, 235);
    if (
      biome == ICE_SHEET_MOUNTAINS || biome == ICE_SHEET_VOLCANIC_MOUNTAINS
    ) return new Color(255, 195, 195);
    if (
      biome == ICE_SHEET_OCEANIC_MOUNTAINS ||
      biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS
    ) return new Color(255, 195, 255);
    if (biome == ICE_SHEET_SHIELD_VOLCANO) return new Color(255, 195, 145);

    if (
      biome == ICE_SHEET_EDGE ||
      biome == ICE_SHEET_SHORE ||
      biome == ICE_SHEET_TUYAS_EDGE
    ) return new Color(165, 165, 165);

    if (biome == STONE_CIRCLES) return new Color(255, 235, 140);
    if (biome == PATTERNED_GROUND) return new Color(255, 215, 110);
    if (biome == INVERTED_PATTERNED_GROUND) return new Color(255, 195, 100);
    if (biome == KNOB_AND_KETTLE) return new Color(235, 175, 80);
    if (biome == DRUMLINS || biome == BURREN_ROCHE_MOUTONEE) return new Color(
      135,
      165,
      135
    );
    if (biome == TUYAS) return new Color(115, 145, 115);
    if (
      biome == GLACIATED_MOUNTAINS || biome == GLACIATED_VOLCANIC_MOUNTAINS
    ) return new Color(255, 165, 165);
    if (
      biome == GLACIATED_OCEANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS
    ) return new Color(255, 165, 255);
    if (biome == GLACIATED_SHIELD_VOLCANO) return new Color(255, 185, 125);
    if (
      biome == GLACIALLY_CARVED_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_MOUNTAINS
    ) return new Color(255, 135, 135);
    if (
      biome == GLACIALLY_CARVED_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
    ) return new Color(255, 135, 255);

    return Color.BLACK;
  }

  // Shows only ice sheets, biomes effected by past ice sheets
  private Color tectonicBiomeColor(int biome) {
    if (biome == OCEAN_REEF) return new Color(150, 160, 255);
    if (biome == OCEAN || biome == OCEAN_ATOLLS) return new Color(
      120,
      120,
      240
    );
    if (biome == OCEAN_RIDGE) return new Color(105, 105, 210);
    if (biome == DEEP_OCEAN || biome == DEEP_OCEAN_ATOLLS) return new Color(
      90,
      90,
      180
    );
    if (biome == DEEP_OCEAN_TRENCH) return new Color(60, 60, 120);
    if (biome == LAKE) return new Color(30, 30, 255);
    if (
      biome == MOUNTAIN_LAKE ||
      biome == OCEANIC_MOUNTAIN_LAKE ||
      biome == OLD_MOUNTAIN_LAKE ||
      biome == VOLCANIC_MOUNTAIN_LAKE ||
      biome == PLATEAU_LAKE
    ) return new Color(20, 180, 255);
    if (biome == RIVER || biome == RIVER_VALLEY) return new Color(0, 200, 255);

    // Convergent Biomes - Note that these show up as regular mtns if they are glaciated
    if (biome == COLLISIONAL_MOUNTAINS) return new Color(205, 160, 200);

    // Volcanic Biomes
    if (
      biome == VOLCANIC_MOUNTAINS ||
      biome == ICE_SHEET_VOLCANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_MOUNTAINS
    ) return new Color(255, 80, 80);
    if (
      biome == VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
    ) return new Color(165, 40, 40);
    if (biome == VOLCANIC_ISLAND) return new Color(185, 160, 30);
    if (biome == VOLCANIC_MOUNTAIN_ISLANDS) return new Color(185, 120, 30);
    if (biome == OCEANIC_VOLCANIC_ARC) return new Color(135, 100, 20);

    // Hotspots
    if (
      biome == ACTIVE_SHIELD_VOLCANO ||
      biome == DORMANT_SHIELD_VOLCANO ||
      biome == EXTINCT_SHIELD_VOLCANO ||
      biome == ANCIENT_SHIELD_VOLCANO ||
      biome == SUNKEN_SHIELD_VOLCANO
    ) return new Color(255, 155, 0);

    // Other Mountain Biomes
    if (
      biome == MOUNTAINS ||
      biome == GLACIATED_MOUNTAINS ||
      biome == GLACIALLY_CARVED_MOUNTAINS ||
      biome == ICE_SHEET_MOUNTAINS
    ) return new Color(0, 220, 40);
    if (
      biome == OCEANIC_MOUNTAINS ||
      biome == GLACIATED_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_OCEANIC_MOUNTAINS ||
      biome == ICE_SHEET_OCEANIC_MOUNTAINS
    ) return new Color(0, 150, 10);
    if (biome == OLD_MOUNTAINS) return new Color(0, 70, 10);

    // Divergent Biomes
    if (biome == RIFT_VALLEY || biome == RIFT_LAKE) return new Color(
      130,
      0,
      110
    );

    return Color.BLACK;
  }

  // Default biome color scheme, Karst Biomes invisible
  private Color heightBiomeColor(int biome) {
    // Oceans
    if (biome == OCEAN_REEF || biome == OCEANIC_VOLCANIC_ARC) return new Color(
      150,
      160,
      255
    );
    if (biome == OCEAN || biome == OCEAN_ATOLLS) return new Color(
      120,
      120,
      240
    );
    if (biome == OCEAN_RIDGE) return new Color(105, 105, 210);
    if (biome == DEEP_OCEAN || biome == DEEP_OCEAN_ATOLLS) return new Color(
      90,
      90,
      180
    );
    if (biome == DEEP_OCEAN_TRENCH) return new Color(60, 60, 120);
    if (biome == LAKE) return new Color(30, 30, 255);
    if (biome == SHORE || biome == TIDAL_FLATS) return new Color(230, 210, 130);
    if (biome == GUANO_ISLAND) return new Color(170, 170, 170);
    if (
      biome == VOLCANIC_ISLAND || biome == VOLCANIC_MOUNTAIN_ISLANDS
    ) return new Color(210, 90, 60);

    // Freshwater
    if (
      biome == MOUNTAIN_LAKE ||
      biome == OCEANIC_MOUNTAIN_LAKE ||
      biome == OLD_MOUNTAIN_LAKE ||
      biome == VOLCANIC_MOUNTAIN_LAKE ||
      biome == PLATEAU_LAKE ||
      biome == RIFT_LAKE
    ) return new Color(120, 170, 200);
    if (biome == RIVER || biome == RIVER_VALLEY) return new Color(0, 200, 255);

    // Lowland / Mixed Water
    if (biome == LOWLANDS || biome == TOWER_KARST_LAKE) return new Color(
      80,
      170,
      200
    );
    if (biome == TOWER_KARST_BAY || biome == SALT_MARSH) return new Color(
      60,
      140,
      220
    );
    if (biome == LOW_CANYONS) return new Color(110, 220, 255);

    // Low lands without water
    if (biome == SALT_FLATS) return new Color(190, 250, 190);
    if (biome == MUD_FLATS) return new Color(150, 200, 130);
    if (
      biome == PLAINS ||
      biome == BURREN_PLAINS ||
      biome == TOWER_KARST_PLAINS ||
      biome == DOLINE_PLAINS ||
      biome == CENOTE_PLAINS ||
      biome == SHILIN_PLAINS ||
      biome == PATTERNED_GROUND ||
      biome == INVERTED_PATTERNED_GROUND ||
      biome == STONE_CIRCLES ||
      biome == KNOB_AND_KETTLE
    ) return new Color(110, 190, 110);
    if (biome == RIFT_VALLEY) return new Color(80, 0, 80);

    // Hills
    if (
      biome == HILLS ||
      biome == SHILIN_HILLS ||
      biome == TOWER_KARST_HILLS ||
      biome == DOLINE_HILLS ||
      biome == CENOTE_HILLS
    ) return new Color(80, 130, 90);
    if (biome == DUNE_SEA || biome == GRASSY_DUNES) return new Color(
      80,
      130,
      90
    );
    if (biome == BADLANDS || biome == BURREN_BADLANDS) return new Color(
      30,
      190,
      30
    );

    // Rolling Hills
    if (
      biome == CANYONS ||
      biome == TOWER_KARST_CANYONS ||
      biome == SHILIN_CANYONS ||
      biome == DOLINE_CANYONS ||
      biome == CENOTE_CANYONS ||
      biome == TUYAS
    ) return new Color(200, 150, 10);
    if (biome == BUTTES || biome == MESAS) return new Color(210, 120, 10);
    if (
      biome == ROLLING_HILLS ||
      biome == DOLINE_ROLLING_HILLS ||
      biome == CENOTE_ROLLING_HILLS ||
      biome == BURREN_ROCHE_MOUTONEE ||
      biome == DRUMLINS
    ) return new Color(240, 230, 10);

    // Highlands
    if (biome == BURREN_BADLANDS_TALL) return new Color(200, 90, 0);
    if (
      biome == STAIR_STEP_CANYONS ||
      biome == HOODOOS ||
      biome == WHORLED_CANYONS
    ) return new Color(230, 110, 0);
    if (
      biome == HIGHLANDS ||
      biome == SHILIN_HIGHLANDS ||
      biome == TOWER_KARST_HIGHLANDS ||
      biome == DOLINE_HIGHLANDS ||
      biome == CENOTE_HIGHLANDS
    ) return new Color(250, 120, 0);

    // Plateau
    if (
      biome == PLATEAU ||
      biome == PLATEAU_WIDE ||
      biome == EXTREME_DOLINE_PLATEAU ||
      biome == CENOTE_PLATEAU ||
      biome == DOLINE_PLATEAU ||
      biome == BURREN_PLATEAU ||
      biome == SHILIN_PLATEAU ||
      biome == ROCKY_PLATEAU
    ) return new Color(200, 60, 60);

    // Mountains
    if (
      biome == OCEANIC_MOUNTAINS || biome == VOLCANIC_OCEANIC_MOUNTAINS
    ) return new Color(160, 30, 160);
    if (
      biome == OLD_MOUNTAINS || biome == EXTREME_DOLINE_MOUNTAINS
    ) return new Color(200, 50, 200);
    if (biome == MOUNTAINS || biome == VOLCANIC_MOUNTAINS) return new Color(
      250,
      10,
      250
    );
    if (biome == COLLISIONAL_MOUNTAINS) return new Color(255, 100, 200);

    // Shield Volcanoes
    if (
      biome == ACTIVE_SHIELD_VOLCANO ||
      biome == DORMANT_SHIELD_VOLCANO ||
      biome == EXTINCT_SHIELD_VOLCANO ||
      biome == ANCIENT_SHIELD_VOLCANO ||
      biome == SUNKEN_SHIELD_VOLCANO
    ) return new Color(250, 90, 250);

    // Ice Sheets - Ice sheet influenced biomes are distributed among other biomes in this view
    if (biome == ICE_SHEET || biome == ICE_SHEET_TUYAS) return new Color(
      255,
      255,
      255
    );
    if (biome == ICE_SHEET_OCEANIC) return new Color(245, 245, 245);
    if (
      biome == ICE_SHEET_MOUNTAINS ||
      biome == ICE_SHEET_MOUNTAINS_EDGE ||
      biome == ICE_SHEET_SHIELD_VOLCANO ||
      biome == GLACIATED_SHIELD_VOLCANO ||
      biome == GLACIATED_MOUNTAINS ||
      biome == GLACIALLY_CARVED_MOUNTAINS ||
      biome == ICE_SHEET_VOLCANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_MOUNTAINS
    ) return new Color(250, 160, 250);
    if (
      biome == ICE_SHEET_OCEANIC_MOUNTAINS ||
      biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == ICE_SHEET_OCEANIC_MOUNTAINS_EDGE ||
      biome == GLACIATED_OCEANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
    ) return new Color(250, 180, 250);
    if (biome == ICE_SHEET_EDGE) return new Color(185, 185, 185);

    return Color.BLACK;
  }

  /**
   * Colors matched to the map on the <a href="https://en.wikipedia.org/wiki/K%C3%B6ppen_climate_classification#/media/File:Koppen-Geiger_Map_v2_World_1991%E2%80%932020.svg">Koppen Climate Wikipedia</a> page.
   */
  private Color koppenClimateColor(KoppenClimateClassification koppen) {
    return switch (koppen) {
      case AF -> new Color(0, 0, 220);
      case AS -> new Color(0, 100, 240);
      case AW -> new Color(0, 150, 220);
      case AM -> new Color(40, 80, 200);
      case BWH -> new Color(210, 0, 0);
      case BSH -> new Color(210, 120, 0);
      case BWK -> new Color(200, 80, 80);
      case BSK -> new Color(200, 120, 60);
      case CSA -> new Color(250, 250, 0);
      case CSB -> new Color(180, 180, 0);
      case CSC -> new Color(120, 120, 0);
      case CWA -> new Color(100, 240, 130);
      case CWB -> new Color(80, 210, 120);
      case CWC -> new Color(70, 160, 110);
      case CFA -> new Color(170, 240, 90);
      case CFB -> new Color(140, 200, 80);
      case CFC -> new Color(110, 170, 70);
      case DSA -> new Color(190, 20, 190);
      case DSB -> new Color(160, 20, 180);
      case DSC -> new Color(130, 20, 170);
      case DSD -> new Color(100, 20, 160);
      case DFA -> new Color(40, 190, 190);
      case DFB -> new Color(30, 170, 170);
      case DFC -> new Color(20, 150, 140);
      case DFD -> new Color(10, 130, 110);
      case DWA -> new Color(80, 80, 220);
      case DWB -> new Color(70, 70, 190);
      case DWC -> new Color(60, 60, 160);
      case DWD -> new Color(60, 60, 130);
      case ET -> new Color(190, 190, 190);
      case EF -> new Color(80, 80, 80);
    };
  }

  private Color hotspot(int age) {
    if (age == 4) return new Color(190, 180, 0);
    if (age == 3) return new Color(220, 110, 0);
    if (age == 2) return new Color(240, 20, 0);
    if (age == 1) return new Color(240, 0, 180);

    return new Color(150, 240, 150);
  }

  /**
   * Allows drawing additional visualizations between generation tasks.
   */
  enum DrawnTask {
    ADD_CONTINENTS(Task.ADD_CONTINENTS),
    ANNOTATE_DISTANCE_TO_CELL_EDGE(Task.ANNOTATE_DISTANCE_TO_CELL_EDGE),
    FLOOD_FILL_SMALL_OCEANS(Task.FLOOD_FILL_SMALL_OCEANS),
    ANNOTATE_BOUNDARY_TYPES(Task.FLOOD_FILL_SMALL_OCEANS),
    ANNOTATE_HOT_SPOT_AGE(Task.ADD_HOTSPOTS),
    ADD_ISLANDS(Task.ADD_ISLANDS),
    ANNOTATE_DISTANCE_TO_OCEAN_AND_DEEP_OCEAN(
      Task.ANNOTATE_DISTANCE_TO_DEEP_OCEAN
    ),
    ANNOTATE_BASE_LAND_HEIGHT(Task.ANNOTATE_BASE_LAND_HEIGHT),
    ANNOTATE_DISTANCE_TO_WEST_COAST(Task.ANNOTATE_DISTANCE_TO_WEST_COAST),
    ADD_MOUNTAINS(Task.ADD_MOUNTAINS),
    ANNOTATE_BIOME_ALTITUDE(Task.ANNOTATE_BIOME_ALTITUDE),
    // Multiple steps to draw temperature, rainfall, and rainfall variance
    TEMPERATURE(Task.ANNOTATE_CLIMATE),
    RAINFALL(Task.ANNOTATE_CLIMATE),
    RAINFALL_VARIANCE(Task.ANNOTATE_CLIMATE),
    KOPPEN(Task.ANNOTATE_CLIMATE),
    CHOOSE_ROCKS(Task.CHOOSE_ROCKS),
    ANNOTATE_KARST_SURFACE(Task.ANNOTATE_KARST_SURFACE),
    CHOOSE_BIOMES(Task.CHOOSE_BIOMES),
    ANNOTATE_BIOMES_BY_HEIGHT(Task.CHOOSE_BIOMES),
    ANNOTATE_KARST_BIOMES(Task.CHOOSE_BIOMES),
    ANNOTATE_GLACIAL_BIOMES(Task.CHOOSE_BIOMES),
    ANNOTATE_TECTONIC_BIOMES(Task.CHOOSE_BIOMES),
    ADD_RIVERS_AND_LAKES(Task.ADD_RIVERS_AND_LAKES),
    // Draw climate visualizations again after rivers, which modify rainfall
    RAINFALL_AFTER_RIVERS(Task.ADD_RIVERS_AND_LAKES),
    KOPPEN_AFTER_RIVERS(Task.ADD_RIVERS_AND_LAKES),
    // Visualize where things can spawn
    KAOLINITE_CAN_SPAWN(Task.ADD_RIVERS_AND_LAKES),
    PROJECTION_GRID(Task.ADD_RIVERS_AND_LAKES);

    final Task root;

    DrawnTask(Task root) {
      this.root = root;
    }
  }
}
