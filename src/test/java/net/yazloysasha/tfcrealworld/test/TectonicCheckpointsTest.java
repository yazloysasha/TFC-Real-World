package net.yazloysasha.tfcrealworld.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.test.drawing.BuiltinWorldPreset;
import net.yazloysasha.tfcrealworld.util.geography.WaypointCoordinates;
import org.junit.jupiter.api.Test;

/**
 * Real-world control points (cities and natural features) checked against the
 * region points produced by the full map pipeline. Expectations describe what
 * vanilla TFC should end up choosing there (relief band, mountain type,
 * seafloor, volcanism), so a failure means the tectonics data or its mapping
 * onto vanilla fields is wrong.
 */
public class TectonicCheckpointsTest implements TestSetup {

  private static final long[] SEEDS = { 20260930L, 7L, 123456789L, -42L };
  private static final String CHECKPOINTS = "/tectonics_checkpoints.json";
  private static final String WAYPOINTS =
    "/data/tfc_real_world/geography/default/waypoints/";

  private static final Map<String, Predicate<Region.Point>> EXPECTATIONS =
    expectations();

  @Test
  public void checkpoints() {
    final List<Checkpoint> checkpoints = loadCheckpoints();
    int failures = 0;
    final StringBuilder report = new StringBuilder();
    for (final long seed : SEEDS) {
      final RegionGenerator generator = new RegionGenerator(
        BuiltinWorldPreset.defaultSettings(),
        Seed.of(seed)
      );
      int seedFailures = 0;
      report
        .append("\n=== Tectonic checkpoints, seed ")
        .append(seed)
        .append(" ===\n");
      for (final Checkpoint checkpoint : checkpoints) {
        final Result result = evaluate(generator, checkpoint);
        if (!result.failed.isEmpty()) {
          seedFailures++;
        }
        report.append(result.format()).append('\n');
      }
      report
        .append("Failed: ")
        .append(seedFailures)
        .append(" / ")
        .append(checkpoints.size())
        .append('\n');
      failures += seedFailures;
    }
    System.out.println(report);
    assertEquals(0, failures, "Tectonic checkpoints failed, see report above");
  }

  /**
   * Whole ranges, plains and seas: every region cell along a path (crest
   * line) or inside a lat/lon box is sampled, and the share of cells meeting
   * each expectation must stay within bounds. Catches a single lucky pixel on
   * an otherwise missing massif.
   */
  @Test
  public void areas() {
    final List<Area> areas = loadAreas();
    int failures = 0;
    final StringBuilder report = new StringBuilder();
    for (final long seed : SEEDS) {
      final RegionGenerator generator = new RegionGenerator(
        BuiltinWorldPreset.defaultSettings(),
        Seed.of(seed)
      );
      int seedFailures = 0;
      report
        .append("\n=== Tectonic areas, seed ")
        .append(seed)
        .append(" ===\n");
      for (final Area area : areas) {
        final String line = evaluateArea(generator, area);
        if (line.startsWith("FAIL")) {
          seedFailures++;
        }
        report.append(line).append('\n');
      }
      report
        .append("Failed: ")
        .append(seedFailures)
        .append(" / ")
        .append(areas.size())
        .append('\n');
      failures += seedFailures;
    }
    System.out.println(report);
    assertEquals(0, failures, "Tectonic areas failed, see report above");
  }

  private static String evaluateArea(RegionGenerator generator, Area area) {
    final List<Region.Point> points = new ArrayList<>();
    for (final long key : area.cells()) {
      final Region.Point point = generator.getOrCreateRegionPoint(
        (int) (key >> 32),
        (int) key
      );
      if (area.filter == null || EXPECTATIONS.get(area.filter).test(point)) {
        points.add(point);
      }
    }
    final List<String> failed = new ArrayList<>();
    final StringBuilder shares = new StringBuilder();
    if (points.size() < area.minCells) {
      failed.add("cells " + points.size() + "<" + area.minCells);
    }
    for (final Bound bound : area.bounds) {
      final Predicate<Region.Point> predicate = EXPECTATIONS.get(bound.name);
      if (predicate == null) {
        throw new IllegalArgumentException(
          "Unknown expectation '" + bound.name + "' in area " + area.name
        );
      }
      final double share = points.isEmpty()
        ? 0
        : points.stream().filter(predicate).count() / (double) points.size();
      final boolean ok = bound.min
        ? share >= bound.value
        : share <= bound.value;
      shares.append(
        String.format(
          Locale.ROOT,
          " %s%s%.2f=%.2f",
          bound.name,
          bound.min ? ">=" : "<=",
          bound.value,
          share
        )
      );
      if (!ok) {
        failed.add(bound.name);
      }
    }
    final Map<String, Integer> histogram = new HashMap<>();
    for (final Region.Point point : points) {
      histogram.merge(name(point), 1, Integer::sum);
    }
    final String top = histogram
      .entrySet()
      .stream()
      .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
      .limit(5)
      .map(e -> e.getKey() + ":" + e.getValue())
      .reduce((l, r) -> l + " " + r)
      .orElse("");
    return String.format(
      Locale.ROOT,
      "%s %-34s n=%4d%s | %s%s",
      failed.isEmpty() ? "PASS" : "FAIL",
      area.name,
      points.size(),
      shares,
      top,
      failed.isEmpty() ? "" : "  MISSED " + failed
    );
  }

  private static Result evaluate(
    RegionGenerator generator,
    Checkpoint checkpoint
  ) {
    final int[] block = WaypointCoordinates.toBlockXZ(
      checkpoint.lat,
      checkpoint.lon
    );
    final int gridX = Units.blockToGrid(block[0]);
    final int gridZ = Units.blockToGrid(block[1]);
    Region.Point point = generator.getOrCreateRegionPoint(gridX, gridZ);
    String shifted = "";

    final Boolean wantLand = checkpoint.expect.contains("land")
      ? Boolean.TRUE
      : checkpoint.expect.contains("water")
        ? Boolean.FALSE
        : null;
    if (wantLand != null && point.land() != wantLand) {
      final Region.Point nearby = nearestMatching(
        generator,
        gridX,
        gridZ,
        wantLand
      );
      if (nearby != null) {
        shifted = " (shifted %+d,%+d)".formatted(
          nearby.x - gridX,
          nearby.z - gridZ
        );
        point = nearby;
      }
    }

    final List<String> failed = new ArrayList<>();
    for (final String expectation : checkpoint.expect) {
      final Predicate<Region.Point> predicate = EXPECTATIONS.get(expectation);
      if (predicate == null) {
        throw new IllegalArgumentException(
          "Unknown expectation '" + expectation + "' at " + checkpoint.name
        );
      }
      if (!predicate.test(point)) {
        failed.add(expectation);
      }
    }
    return new Result(checkpoint, point, shifted, failed);
  }

  private static Region.Point nearestMatching(
    RegionGenerator generator,
    int gridX,
    int gridZ,
    boolean land
  ) {
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        final Region.Point candidate = generator.getOrCreateRegionPoint(
          gridX + dx,
          gridZ + dz
        );
        if (candidate.land() == land) {
          return candidate;
        }
      }
    }
    return null;
  }

  private static Map<String, Predicate<Region.Point>> expectations() {
    final Map<String, Predicate<Region.Point>> map = new HashMap<>();
    map.put("land", Region.Point::land);
    map.put("water", p -> !p.land());
    map.put("mountains", p -> p.land() && p.mountain());
    map.put("not_mountain", p -> !p.mountain());
    map.put("lowland", p -> relief(p) == 0);
    map.put("upland", p -> relief(p) == 1);
    map.put("highland", p -> relief(p) == 2);
    map.put("not_lowland", p -> relief(p) >= 1);
    map.put("upland_or_highland", p -> relief(p) == 1 || relief(p) == 2);
    map.put("highland_or_mountain", p -> relief(p) >= 2);
    map.put("not_highland_or_mountain", p -> relief(p) <= 1);
    map.put("old_mountains", p -> relief(p) == 2 && !p.volcanic());
    map.put(
      "volcanic_mountains",
      p -> p.mountain() && name(p).contains("VOLCANIC")
    );
    map.put("not_volcanic_mountains", p -> !name(p).contains("VOLCANIC"));
    map.put(
      "collisional",
      p ->
        p.mountain() &&
        p.divergence < 0 &&
        name(p).contains("MOUNTAIN") &&
        !name(p).contains("VOLCANIC")
    );
    map.put(
      "volcanic",
      p ->
        p.volcanic() ||
        p.hotSpotAge > 0 ||
        name(p).contains("VOLCAN") ||
        name(p).contains("SHIELD")
    );
    // Vanilla picks an island's biome at random from its island biomes, some
    // of them volcanic-looking, so on islands only the flags tell volcanism.
    map.put(
      "not_volcanic",
      p ->
        !p.volcanic() &&
        p.hotSpotAge == 0 &&
        (p.island() || !name(p).contains("VOLCAN"))
    );
    map.put("hotspot", p -> p.hotSpotAge > 0 && name(p).contains("SHIELD"));
    map.put("not_hotspot", p -> p.hotSpotAge == 0);
    // A map lake inside a rift zone becomes vanilla lakeFor(RIFT_VALLEY).
    map.put(
      "rift",
      p ->
        name(p).contains("RIFT") ||
        (p.lake() && p.divergence > 0 && p.distanceToEdge < 3)
    );
    map.put("not_rift", p -> !name(p).contains("RIFT"));
    map.put("ridge", p -> p.biome == TFCLayers.OCEAN_RIDGE);
    map.put("trench", p -> p.biome == TFCLayers.DEEP_OCEAN_TRENCH);
    map.put("shelf", p -> !p.land() && p.oceanDepth == 2);
    map.put("deep", p -> !p.land() && p.oceanDepth == 4);
    map.put("oceanic_mountains", p -> name(p).contains("OCEANIC_MOUNTAIN"));
    // The coastal (oceanic) mountain flag itself: a hotspot shield or tower
    // karst may still replace the biome of such a cell.
    map.put("coastal_mountains", p -> p.mountain() && p.coastalMountain());
    map.put("not_oceanic_mountains", p ->
      !name(p).contains("OCEANIC_MOUNTAIN")
    );
    map.put("old_mountain_band", p -> relief(p) >= 2 && !p.volcanic());
    map.put("any", p -> true);
    map.put("reef", p -> p.biome == TFCLayers.OCEAN_REEF);
    map.put(
      "reef_or_island",
      p -> p.biome == TFCLayers.OCEAN_REEF || p.island()
    );
    map.put(
      "shallow",
      p -> !p.land() && (p.oceanDepth == 2 || p.biome == TFCLayers.OCEAN_REEF)
    );
    return map;
  }

  /** -1 water or rift, 0 low, 1 mid, 2 high, 3 mountain. */
  private static int relief(Region.Point point) {
    if (!point.land() || name(point).contains("RIFT")) {
      return -1;
    }
    if (point.mountain()) {
      return 3;
    }
    return Math.min(2, point.discreteBiomeAltitude());
  }

  private static String name(Region.Point point) {
    return TfcBiomeIds.name(point.biome);
  }

  private static List<Checkpoint> loadCheckpoints() {
    final JsonObject root = readJson(CHECKPOINTS);
    final List<Checkpoint> out = new ArrayList<>();
    for (final JsonElement element : root.getAsJsonArray("cities")) {
      final JsonObject json = element.getAsJsonObject();
      final String id = json.get("waypoint").getAsString();
      final JsonObject waypoint = readJsonOrNull(WAYPOINTS + id + ".json");
      if (waypoint == null) {
        if (json.has("optional") && json.get("optional").getAsBoolean()) {
          continue;
        }
        throw new IllegalArgumentException("Unknown waypoint " + id);
      }
      out.add(
        new Checkpoint(
          id,
          waypoint.get("latitude").getAsDouble(),
          waypoint.get("longitude").getAsDouble(),
          strings(json.getAsJsonArray("expect"))
        )
      );
    }
    for (final JsonElement element : root.getAsJsonArray("nature")) {
      final JsonObject json = element.getAsJsonObject();
      out.add(
        new Checkpoint(
          json.get("name").getAsString(),
          json.get("lat").getAsDouble(),
          json.get("lon").getAsDouble(),
          strings(json.getAsJsonArray("expect"))
        )
      );
    }
    return out;
  }

  private static List<Area> loadAreas() {
    final List<Area> out = new ArrayList<>();
    for (final JsonElement element : readJson(CHECKPOINTS).getAsJsonArray(
      "areas"
    )) {
      final JsonObject json = element.getAsJsonObject();
      final List<Bound> bounds = new ArrayList<>();
      for (final String kind : List.of("min", "max")) {
        if (json.has(kind)) {
          for (final var entry : json.getAsJsonObject(kind).entrySet()) {
            bounds.add(
              new Bound(
                entry.getKey(),
                kind.equals("min"),
                entry.getValue().getAsDouble()
              )
            );
          }
        }
      }
      final List<double[]> path = new ArrayList<>();
      if (json.has("path")) {
        for (final JsonElement vertex : json.getAsJsonArray("path")) {
          final JsonArray pair = vertex.getAsJsonArray();
          path.add(
            new double[] {
              pair.get(0).getAsDouble(),
              pair.get(1).getAsDouble(),
            }
          );
        }
      }
      double[] box = null;
      if (json.has("box")) {
        final JsonArray b = json.getAsJsonArray("box");
        box = new double[] {
          b.get(0).getAsDouble(),
          b.get(1).getAsDouble(),
          b.get(2).getAsDouble(),
          b.get(3).getAsDouble(),
        };
      }
      out.add(
        new Area(
          json.get("name").getAsString(),
          path,
          box,
          json.has("filter") ? json.get("filter").getAsString() : "land",
          json.has("min_cells") ? json.get("min_cells").getAsInt() : 5,
          bounds
        )
      );
    }
    return out;
  }

  private static List<String> strings(JsonArray array) {
    final List<String> out = new ArrayList<>();
    for (final JsonElement element : array) {
      out.add(element.getAsString());
    }
    return out;
  }

  private static JsonObject readJson(String path) {
    final JsonObject json = readJsonOrNull(path);
    if (json == null) {
      throw new IllegalStateException("Missing test resource " + path);
    }
    return json;
  }

  private static JsonObject readJsonOrNull(String path) {
    try (
      InputStream stream = TectonicCheckpointsTest.class.getResourceAsStream(
        path
      )
    ) {
      if (stream == null) {
        return null;
      }
      return JsonParser.parseReader(
        new InputStreamReader(stream, StandardCharsets.UTF_8)
      ).getAsJsonObject();
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private record Bound(String name, boolean min, double value) {}

  private record Area(
    String name,
    List<double[]> path,
    double[] box,
    String filter,
    int minCells,
    List<Bound> bounds
  ) {
    private static final double STEP_DEG = 0.1;

    /** Distinct region grid cells covered by the path or the box. */
    java.util.Set<Long> cells() {
      final java.util.Set<Long> cells = new java.util.LinkedHashSet<>();
      if (box != null) {
        for (double lat = box[0]; lat <= box[2]; lat += STEP_DEG) {
          for (double lon = box[1]; lon <= box[3]; lon += STEP_DEG) {
            cells.add(cell(lat, lon));
          }
        }
      }
      for (int i = 0; i + 1 < path.size(); i++) {
        final double[] a = path.get(i);
        final double[] b = path.get(i + 1);
        final int steps = (int) Math.ceil(
          Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1])) / STEP_DEG
        );
        for (int k = 0; k <= steps; k++) {
          final double t = steps == 0 ? 0 : k / (double) steps;
          cells.add(cell(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t));
        }
      }
      return cells;
    }

    private static long cell(double lat, double lon) {
      final int[] block = WaypointCoordinates.toBlockXZ(lat, lon);
      final long x = Units.blockToGrid(block[0]);
      final long z = Units.blockToGrid(block[1]);
      return (x << 32) | (z & 0xffffffffL);
    }
  }

  private record Checkpoint(
    String name,
    double lat,
    double lon,
    List<String> expect
  ) {}

  private record Result(
    Checkpoint checkpoint,
    Region.Point point,
    String shifted,
    List<String> failed
  ) {
    String format() {
      return String.format(
        Locale.ROOT,
        "%s %-34s %-44s alt=%d mtn=%s cst=%s vol=%s div=%+.0f edge=%3d ocean=%3d depth=%d hot=%d -> %s%s",
        failed.isEmpty() ? "PASS" : "FAIL",
        checkpoint.name,
        String.join(",", checkpoint.expect),
        point.discreteBiomeAltitude(),
        point.mountain() ? "Y" : "-",
        point.coastalMountain() ? "Y" : "-",
        point.volcanic() ? "Y" : "-",
        point.divergence,
        point.distanceToEdge,
        point.distanceToOcean,
        point.oceanDepth,
        point.hotSpotAge,
        name(point),
        shifted + (failed.isEmpty() ? "" : "  MISSED " + failed)
      );
    }
  }
}
