package net.yazloysasha.tfcrealworld.test.drawing;

import static net.dries007.tfc.world.layer.TFCLayers.RIVER;

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
import net.yazloysasha.tfcrealworld.test.TfcBiomeIds;
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

  /**
   * Samples per grid cell side. One sample per cell (128 blocks) misses
   * biomes smaller than a cell (islets, shore rings, lone summits).
   */
  private static final int SAMPLES_PER_CELL_SIDE = 4;

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

  /**
   * One pass over the whole map for a few fixed seeds: how many samples
   * (4×4 per grid cell) end up with each biome on the final layer, rarest
   * first. Run with
   * {@code ./gradlew test -PbiomeCoverageOnce --tests "*BiomeCoverageContinuousTest"}.
   */
  @Test
  @EnabledIfSystemProperty(named = "biomeCoverageOnce", matches = "true")
  @Timeout(value = 2, unit = TimeUnit.HOURS)
  public void biomeCoverageCounts() {
    final long[] seeds = { 20260930L, 7L, 123456789L };
    final Map<Integer, int[]> counts = new HashMap<>();
    for (int i = 0; i < seeds.length; i++) {
      final Map<Integer, Integer> seedCounts = countBiomes(
        seeds[i],
        0,
        0,
        MAP_RADIUS
      );
      if (seedCounts == null) {
        continue;
      }
      final int index = i;
      for (final int biome : getAllPossibleBiomes()) {
        counts.computeIfAbsent(biome, b -> new int[seeds.length])[index] =
          seedCounts.getOrDefault(biome, 0);
      }
    }
    final StringBuilder out = new StringBuilder(
      "\n=== Biome layer counts per seed ===\n"
    );
    counts
      .entrySet()
      .stream()
      .sorted(
        java.util.Comparator.comparingInt(e ->
          java.util.Arrays.stream(e.getValue()).min().orElse(0)
        )
      )
      .forEach(e ->
        out
          .append(String.format(Locale.ROOT, "%-44s", getBiomeName(e.getKey())))
          .append(java.util.Arrays.toString(e.getValue()))
          .append('\n')
      );
    System.out.print(out);
  }

  /**
   * Many random seeds, with the count of every biome on the final layer kept
   * for each: how often a biome is missing and how close to missing the rare
   * ones come. Counts go to build/minecraft-junit/biome_coverage_runs.csv,
   * one row per seed.
   * Run with {@code ./gradlew test -PbiomeCoverageRuns=100 --tests
   * "*BiomeCoverageContinuousTest"}.
   */
  @Test
  @EnabledIfSystemProperty(named = "biomeCoverageRuns", matches = "\\d+")
  @Timeout(value = 12, unit = TimeUnit.HOURS)
  public void biomeCoverageRuns() throws java.io.IOException {
    final int runs = Integer.getInteger("biomeCoverageRuns");
    final java.util.List<Integer> biomes = getAllPossibleBiomes()
      .stream()
      .sorted()
      .toList();
    final Map<Integer, int[]> counts = new HashMap<>();
    for (final int biome : biomes) {
      counts.put(biome, new int[runs]);
    }
    final StringBuilder csv = new StringBuilder("seed");
    for (final int biome : biomes) {
      csv.append(',').append(getBiomeName(biome));
    }
    csv.append('\n');
    for (int run = 0; run < runs; ) {
      final long seed = RandomSupport.generateUniqueSeed();
      final Map<Integer, Integer> seedCounts = countBiomes(
        seed,
        0,
        0,
        MAP_RADIUS
      );
      if (seedCounts == null) {
        continue;
      }
      csv.append(seed);
      for (final int biome : biomes) {
        final int count = seedCounts.getOrDefault(biome, 0);
        counts.get(biome)[run] = count;
        csv.append(',').append(count);
      }
      csv.append('\n');
      run++;
      java.nio.file.Files.writeString(
        java.nio.file.Path.of("biome_coverage_runs.csv"),
        csv
      );
      System.out.println("Biome coverage run " + run + " of " + runs);
    }
    final StringBuilder out = new StringBuilder(
      "\n=== Biome layer samples over " +
        runs +
        " seeds: missing runs, min, 5th percentile, median ===\n"
    );
    counts
      .entrySet()
      .stream()
      .sorted(
        java.util.Comparator.comparingInt(e ->
          java.util.Arrays.stream(e.getValue()).sorted().toArray()[runs / 20]
        )
      )
      .forEach(e -> {
        final int[] sorted = java.util.Arrays.stream(e.getValue())
          .sorted()
          .toArray();
        final long missing = java.util.Arrays.stream(sorted)
          .filter(count -> count == 0)
          .count();
        out.append(
          String.format(
            Locale.ROOT,
            "%-44s %3d %7d %7d %7d%n",
            getBiomeName(e.getKey()),
            missing,
            sorted[0],
            sorted[runs / 20],
            sorted[runs / 2]
          )
        );
      });
    System.out.print(out);
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

  private Map<Integer, Integer> countBiomes(
    long seed,
    int centerX,
    int centerZ,
    int radius
  ) {
    final Settings settings = BuiltinWorldPreset.defaultSettings();
    final RegionGenerator generator = new RegionGenerator(
      settings,
      Seed.of(seed)
    );
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
    final Area biomeLayer = TFCLayers.createRegionBiomeLayer(
      generator,
      Seed.of(seed)
    ).get();
    final Map<Integer, Integer> counts = new HashMap<>();
    for (int dx = 0; dx < radius * 2; dx++) {
      for (int dz = 0; dz < radius * 2; dz++) {
        final int gridX = centerX - radius + dx;
        final int gridZ = centerZ - radius + dz;
        if (!MapTileGridBounds.isInsidePrimaryMapTile(gridX, gridZ)) {
          continue;
        }
        forEachCellSample(generator, biomeLayer, gridX, gridZ, biome ->
          counts.merge(biome, 1, Integer::sum)
        );
      }
    }
    return counts;
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
          forEachCellSample(generator, biomeLayer, gridX, gridZ, present::add);
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

  private void forEachCellSample(
    RegionGenerator generator,
    Area biomeLayer,
    int gridX,
    int gridZ,
    java.util.function.IntConsumer consumer
  ) {
    final int step = Units.GRID_WIDTH_IN_BLOCK / SAMPLES_PER_CELL_SIDE;
    for (int sx = 0; sx < SAMPLES_PER_CELL_SIDE; sx++) {
      for (int sz = 0; sz < SAMPLES_PER_CELL_SIDE; sz++) {
        consumer.accept(
          resolveWorldBiomeLayerId(
            generator,
            biomeLayer,
            gridX,
            gridZ,
            Units.gridToBlock(gridX) + sx * step + (step >> 1),
            Units.gridToBlock(gridZ) + sz * step + (step >> 1)
          )
        );
      }
    }
  }

  private int resolveWorldBiomeLayerId(
    RegionGenerator generator,
    Area biomeLayer,
    int gridX,
    int gridZ,
    int blockX,
    int blockZ
  ) {
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

  private Set<Integer> getAllPossibleBiomes() {
    return TfcBiomeIds.names().keySet();
  }

  private String getBiomeName(int biome) {
    return TfcBiomeIds.name(biome);
  }
}
