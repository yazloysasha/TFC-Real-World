package net.yazloysasha.tfcrealworld.test.drawing;

import static net.dries007.tfc.world.layer.TFCLayers.RIVER;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.RiverEdge;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.settings.Settings;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.yazloysasha.tfcrealworld.test.TestSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs world generation in a loop and prints cumulative counts of how often each
 * biome was absent from the map. Stop manually when the sample is large enough.
 *
 * <p>Skipped during normal {@code ./gradlew test} / build. Run explicitly with
 * {@code ./gradlew test -PcontinuousBiomeCoverage --tests
 * net.yazloysasha.tfcrealworld.test.drawing.BiomeCoverageContinuousTest}
 * (stop with Ctrl+C).
 */
public class BiomeCoverageContinuousTest implements TestSetup {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    BiomeCoverageContinuousTest.class
  );

  private static final int MAP_RADIUS = 312;

  /**
   * Extra grid cells to generate so biome layers can read adjacent region points.
   */
  private static final int REGION_LAYER_PADDING = 6;

  @Test
  @EnabledIfSystemProperty(named = "continuousBiomeCoverage", matches = "true")
  @Timeout(value = 365, unit = TimeUnit.DAYS)
  public void biomeCoverageContinuous() {
    final Map<Integer, Integer> missingTotals = new HashMap<>();
    int runs = 0;
    int runsWithAnyMissing = 0;

    while (true) {
      final long seed = RandomSupport.generateUniqueSeed();
      final Set<Integer> present = collectPresentBiomes(seed, 0, 0, MAP_RADIUS);
      if (present == null) {
        LOGGER.warn(
          "Skipped seed {} (region/layer sampling hit an orphan grid cell)",
          seed
        );
        continue;
      }
      runs++;
      final Set<Integer> missing = new HashSet<>(getAllPossibleBiomes());
      missing.removeAll(present);

      if (!missing.isEmpty()) {
        runsWithAnyMissing++;
        for (int biome : missing) {
          missingTotals.merge(biome, 1, Integer::sum);
        }
      }

      printCumulativeReport(
        runs,
        runsWithAnyMissing,
        missingTotals,
        seed,
        missing.size()
      );

      if (runs % 5 == 0) {
        System.gc();
      }
    }
  }

  private void printCumulativeReport(
    int runs,
    int runsWithAnyMissing,
    Map<Integer, Integer> missingTotals,
    long lastSeed,
    int lastMissingCount
  ) {
    final StringBuilder out = new StringBuilder();
    out
      .append("\n=== Biome coverage cumulative (")
      .append(runs)
      .append(" runs, radius ")
      .append(MAP_RADIUS)
      .append(") ===\n");
    out
      .append("Last seed: ")
      .append(lastSeed)
      .append(", missing this run: ")
      .append(lastMissingCount)
      .append('\n');
    out
      .append("Runs with at least one missing biome: ")
      .append(runsWithAnyMissing)
      .append(" / ")
      .append(runs);
    if (runs > 0) {
      out
        .append(" (")
        .append(
          String.format(
            Locale.ROOT,
            "%.1f%%",
            (runsWithAnyMissing * 100.0) / runs
          )
        )
        .append(')');
    }
    out.append('\n');

    if (missingTotals.isEmpty()) {
      out.append("No missing biomes recorded yet.\n");
    } else {
      out.append(
        "Total times each biome was missing (sum over runs, sorted by count):\n"
      );
      missingTotals
        .entrySet()
        .stream()
        .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
        .forEach(entry ->
          out
            .append("  ")
            .append(getBiomeName(entry.getKey()))
            .append(": ")
            .append(entry.getValue())
            .append(" / ")
            .append(runs)
            .append(" runs (")
            .append(
              String.format(
                Locale.ROOT,
                "%.1f%%",
                (entry.getValue() * 100.0) / runs
              )
            )
            .append(")\n")
        );
    }
    out.append("====================================================\n");

    System.out.print(out);
    System.out.flush();
  }

  private Set<Integer> collectPresentBiomes(
    long seed,
    int centerX,
    int centerZ,
    int radius
  ) {
    try {
      final Settings settings = BuiltinWorldPreset.defaultSettings();
      final RegionGenerator generator = new RegionGenerator(
        settings,
        Seed.of(seed)
      );
      final int size = radius * 2;
      final boolean[] regionGenerated = new boolean[size * size];

      for (int dx = 0; dx < size; dx++) {
        for (int dz = 0; dz < size; dz++) {
          if (regionGenerated[dx + size * dz]) {
            continue;
          }
          generator.visualizeRegion(
            centerX - radius + dx,
            centerZ - radius + dz,
            (task, region) -> {
              for (Region.Point point : region.points()) {
                final int pointX = point.x - centerX + radius;
                final int pointZ = point.z - centerZ + radius;
                if (
                  pointX >= 0 && pointX < size && pointZ >= 0 && pointZ < size
                ) {
                  regionGenerated[pointX + size * pointZ] = true;
                }
              }
            }
          );
        }
      }

      if (
        !warmRegionPointsForLayerSampling(
          generator,
          centerX,
          centerZ,
          radius,
          REGION_LAYER_PADDING
        )
      ) {
        return null;
      }

      final AreaFactory biomeLayerFactory = TFCLayers.createRegionBiomeLayer(
        generator,
        Seed.of(seed)
      );
      final Area biomeLayer = biomeLayerFactory.get();
      final Set<Integer> present = new HashSet<>();

      for (int dx = 0; dx < size; dx++) {
        for (int dz = 0; dz < size; dz++) {
          final int gridX = centerX - radius + dx;
          final int gridZ = centerZ - radius + dz;
          if (!MapTileGridBounds.isInsidePrimaryMapTile(gridX, gridZ)) {
            continue;
          }
          present.add(
            resolveWorldBiomeLayerId(generator, biomeLayer, gridX, gridZ)
          );
        }
      }
      return present;
    } catch (AssertionError e) {
      LOGGER.warn(
        "Region/layer sampling failed for seed {}: {}",
        seed,
        e.getMessage()
      );
      return null;
    }
  }

  /**
   * Biome layers read neighboring coordinates; TFC can throw if {@code sampleCell(x,z)}
   * returns a region that does not contain {@code (x,z)}. Warm the cache and detect that case.
   */
  private static boolean warmRegionPointsForLayerSampling(
    RegionGenerator generator,
    int centerX,
    int centerZ,
    int radius,
    int padding
  ) {
    try {
      for (
        int gridX = centerX - radius - padding;
        gridX < centerX + radius + padding;
        gridX++
      ) {
        for (
          int gridZ = centerZ - radius - padding;
          gridZ < centerZ + radius + padding;
          gridZ++
        ) {
          generator.getOrCreateRegionPoint(gridX, gridZ);
        }
      }
      return true;
    } catch (AssertionError e) {
      return false;
    }
  }

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

  private static Set<Integer> allPossibleBiomesCache;
  private static Map<Integer, String> biomeNameCache;

  private Set<Integer> getAllPossibleBiomes() {
    if (allPossibleBiomesCache != null) {
      return allPossibleBiomesCache;
    }

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
    if (biomeNameCache != null && biomeNameCache.containsKey(biome)) {
      return biomeNameCache.get(biome);
    }
    return "UNKNOWN_BIOME_" + biome;
  }
}
