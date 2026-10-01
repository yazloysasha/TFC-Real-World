package net.yazloysasha.tfcrealworld.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.minecraft.core.QuartPos;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.test.drawing.BuiltinWorldPreset;
import net.yazloysasha.tfcrealworld.util.geography.WaypointCoordinates;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import org.junit.jupiter.api.Test;

/**
 * Checks on the final biome layer and on the whole-world biome mix, the
 * places where tectonic properties turn into what the player sees.
 */
public class TectonicBiomeLayerTest implements TestSetup {

  private static final long SEED = 20260930L;

  /** Samples per grid cell edge on the biome layer (16 blocks apart). */
  private static final int LAYER_STEP_BLOCKS = 16;

  /** Coastline smear allowed by the zooms after the land/ocean correction. */
  private static final int COAST_TOLERANCE_BLOCKS = 24;

  private static final double[][] ISLAND_BOXES = {
    // south, west, north, east
    { 43.0, 145.0, 51.0, 157.0 }, // Kuril Islands
    { 11.8, -62.5, 18.3, -60.5 }, // Lesser Antilles
    { 51.0, -179.0, 55.0, -163.0 }, // Aleutian Islands
    { 9.0, 121.0, 12.5, 126.0 }, // Visayas
    { 35.5, 23.0, 38.5, 27.5 }, // Aegean Sea
  };

  /**
   * Land biomes stand only on continent.png land; the sea keeps ocean biomes.
   * Mantle-plume shield volcanoes are the only land biomes allowed to reach
   * into the sea.
   */
  @Test
  public void landBiomesOnlyOnMapLand() {
    final RegionGenerator generator = generator();
    final PNGContinentNoise continent = ContinentNoiseRegistry.get(generator);
    final Area biomes = TFCLayers.createRegionBiomeLayer(
      generator,
      Seed.of(SEED)
    ).get();
    final StringBuilder report = new StringBuilder(
      "\n=== Land biomes on map ocean ===\n"
    );
    int total = 0;
    int offending = 0;
    for (final double[] box : ISLAND_BOXES) {
      final int[] a = WaypointCoordinates.toBlockXZ(box[0], box[1]);
      final int[] b = WaypointCoordinates.toBlockXZ(box[2], box[3]);
      final Map<String, Integer> bad = new HashMap<>();
      int boxOcean = 0;
      int boxBad = 0;
      for (
        int x = Math.min(a[0], b[0]);
        x <= Math.max(a[0], b[0]);
        x += LAYER_STEP_BLOCKS
      ) {
        for (
          int z = Math.min(a[1], b[1]);
          z <= Math.max(a[1], b[1]);
          z += LAYER_STEP_BLOCKS
        ) {
          final double gridX = Units.blockToGridExact(x);
          final double gridZ = Units.blockToGridExact(z);
          if (!openSea(continent, gridX, gridZ)) {
            continue;
          }
          boxOcean++;
          final int biome = biomes.get(
            QuartPos.fromBlock(x),
            QuartPos.fromBlock(z)
          );
          if (
            TFCLayers.isOcean(biome) || hotspotNear(generator, gridX, gridZ)
          ) {
            continue;
          }
          boxBad++;
          bad.merge(TfcBiomeIds.name(biome), 1, Integer::sum);
          if (boxBad <= 3) {
            report.append(
              String.format(
                Locale.ROOT,
                "  at block %d,%d grid %.2f,%.2f nearest land within 5 grid: %s%n",
                x,
                z,
                gridX,
                gridZ,
                landWithin(continent, gridX, gridZ)
              )
            );
          }
        }
      }
      total += boxOcean;
      offending += boxBad;
      report.append(
        String.format(
          Locale.ROOT,
          "box %s: ocean samples=%d land biome on ocean=%d %s%n",
          java.util.Arrays.toString(box),
          boxOcean,
          boxBad,
          bad
        )
      );
    }
    System.out.println(report);
    assertEquals(
      0,
      offending,
      "Land biomes painted over continent.png ocean (" + total + " samples)"
    );
  }

  /**
   * Hotspot shields raise land around their centre; the volcanic islands of
   * the southern Red Sea must not close it off from the ocean.
   */
  @Test
  public void redSeaReachesTheOcean() {
    final RegionGenerator generator = generator();
    final Area biomes = TFCLayers.createRegionBiomeLayer(
      generator,
      Seed.of(SEED)
    ).get();
    final int[] corner = WaypointCoordinates.toBlockXZ(26.0, 31.0);
    final int[] opposite = WaypointCoordinates.toBlockXZ(8.0, 52.0);
    final int minX = Math.min(corner[0], opposite[0]) / LAYER_STEP_BLOCKS;
    final int minZ = Math.min(corner[1], opposite[1]) / LAYER_STEP_BLOCKS;
    final int sizeX =
      Math.max(corner[0], opposite[0]) / LAYER_STEP_BLOCKS - minX + 1;
    final int sizeZ =
      Math.max(corner[1], opposite[1]) / LAYER_STEP_BLOCKS - minZ + 1;
    final int[] start = WaypointCoordinates.toBlockXZ(21.0, 38.0);
    final int[] goal = WaypointCoordinates.toBlockXZ(12.5, 47.0);
    final int goalIndex =
      (goal[0] / LAYER_STEP_BLOCKS - minX) +
      (goal[1] / LAYER_STEP_BLOCKS - minZ) * sizeX;

    final boolean[] visited = new boolean[sizeX * sizeZ];
    final ArrayDeque<Integer> queue = new ArrayDeque<>();
    final int startIndex =
      (start[0] / LAYER_STEP_BLOCKS - minX) +
      (start[1] / LAYER_STEP_BLOCKS - minZ) * sizeX;
    visited[startIndex] = true;
    queue.add(startIndex);
    while (!queue.isEmpty() && !visited[goalIndex]) {
      final int index = queue.poll();
      final int x = index % sizeX;
      final int z = index / sizeX;
      for (final int[] step : new int[][] {
        { 1, 0 },
        { -1, 0 },
        { 0, 1 },
        { 0, -1 },
      }) {
        final int nx = x + step[0];
        final int nz = z + step[1];
        if (nx < 0 || nz < 0 || nx >= sizeX || nz >= sizeZ) {
          continue;
        }
        final int next = nx + nz * sizeX;
        if (visited[next]) {
          continue;
        }
        final int biome = biomes.get(
          QuartPos.fromBlock((minX + nx) * LAYER_STEP_BLOCKS),
          QuartPos.fromBlock((minZ + nz) * LAYER_STEP_BLOCKS)
        );
        if (TFCLayers.isOcean(biome)) {
          visited[next] = true;
          queue.add(next);
        }
      }
    }
    assertTrue(
      visited[goalIndex],
      "No sea route from the Red Sea to the Gulf of Aden"
    );
  }

  /** Biomes that must exist somewhere on the real-world map. */
  @Test
  public void worldBiomeMix() {
    final RegionGenerator generator = generator();
    final Map<Integer, Integer> counts = new HashMap<>();
    final int radiusX = Units.blockToGrid(
      TFCRealWorldConfig.HORIZONTAL_SCALE.get()
    );
    final int radiusZ = Units.blockToGrid(
      TFCRealWorldConfig.VERTICAL_SCALE.get()
    );
    for (int x = -radiusX; x < radiusX; x++) {
      for (int z = -radiusZ; z < radiusZ; z++) {
        final Region.Point point = generator.getOrCreateRegionPoint(x, z);
        counts.merge(point.biome, 1, Integer::sum);
      }
    }
    final List<String> failures = new ArrayList<>();
    final StringBuilder report = new StringBuilder(
      "\n=== World biome mix ===\n"
    );
    counts
      .entrySet()
      .stream()
      .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
      .forEach(e ->
        report
          .append(TfcBiomeIds.name(e.getKey()))
          .append(": ")
          .append(e.getValue())
          .append('\n')
      );
    for (final int required : new int[] {
      TFCLayers.OCEAN_REEF,
      TFCLayers.VOLCANIC_ISLAND,
      TFCLayers.VOLCANIC_MOUNTAIN_ISLANDS,
      TFCLayers.GUANO_ISLAND,
      TFCLayers.OCEANIC_VOLCANIC_ARC,
      TFCLayers.VOLCANIC_MOUNTAINS,
      TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS,
      TFCLayers.COLLISIONAL_MOUNTAINS,
      TFCLayers.OCEANIC_MOUNTAINS,
      TFCLayers.OLD_MOUNTAINS,
      TFCLayers.RIFT_VALLEY,
      TFCLayers.OCEAN_RIDGE,
      TFCLayers.DEEP_OCEAN_TRENCH,
      TFCLayers.ACTIVE_SHIELD_VOLCANO,
    }) {
      if (counts.getOrDefault(required, 0) == 0) {
        failures.add("missing " + TfcBiomeIds.name(required));
      }
    }
    System.out.println(report);
    assertEquals(List.of(), failures);
  }

  private static RegionGenerator generator() {
    return new RegionGenerator(
      BuiltinWorldPreset.defaultSettings(),
      Seed.of(SEED)
    );
  }

  /**
   * Map ocean at least COAST_TOLERANCE_BLOCKS from any map land. Right at the
   * coastline the biome layer is zoomed and smoothed after the land/ocean
   * correction, so a pixel of coast biome either way is expected.
   */
  private static boolean openSea(
    PNGContinentNoise continent,
    double gridX,
    double gridZ
  ) {
    if (!continent.isOceanAtGridHard(gridX, gridZ)) {
      return false;
    }
    for (int blocks = 8; blocks <= COAST_TOLERANCE_BLOCKS; blocks += 8) {
      final double radius = Units.blockToGridExact(blocks);
      for (int k = 0; k < 16; k++) {
        final double angle = (k * Math.PI) / 8;
        if (
          !continent.isOceanAtGridHard(
            gridX + radius * Math.cos(angle),
            gridZ + radius * Math.sin(angle)
          )
        ) {
          return false;
        }
      }
    }
    return true;
  }

  private static String landWithin(
    PNGContinentNoise continent,
    double gridX,
    double gridZ
  ) {
    for (int r = 1; r <= 40; r++) {
      final double step = r * 0.125;
      for (int k = 0; k < 16; k++) {
        final double a = (k * Math.PI) / 8;
        if (
          !continent.isOceanAtGridHard(
            gridX + step * Math.cos(a),
            gridZ + step * Math.sin(a)
          )
        ) {
          return String.format(Locale.ROOT, "%.3f grid", step);
        }
      }
    }
    return "none";
  }

  private static boolean hotspotNear(
    RegionGenerator generator,
    double gridX,
    double gridZ
  ) {
    final int gx = (int) Math.floor(gridX);
    final int gz = (int) Math.floor(gridZ);
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        if (generator.getOrCreateRegionPoint(gx + dx, gz + dz).hotSpotAge > 0) {
          return true;
        }
      }
    }
    return false;
  }
}
