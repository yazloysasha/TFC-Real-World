package net.yazloysasha.tfcrealworld.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.RiverEdge;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.test.drawing.BuiltinWorldPreset;
import net.yazloysasha.tfcrealworld.util.geography.WaypointCoordinates;
import org.junit.jupiter.api.Test;

/** Real rivers run where the map has them, and nowhere else. */
public class MapRiversTest implements TestSetup {

  private static final long SEED = 20260930L;

  /**
   * Edges are 2.7 cells long, so a sharp bend is cut by up to a cell, and the
   * edge fractal bends a river about half a cell off its line.
   */
  private static final double RIVER_TOLERANCE_CELLS = 1.5;

  private static final double DRY_DISTANCE_CELLS = 3.0;

  private record Place(String name, double latitude, double longitude) {}

  private static final Place[] ON_RIVERS = {
    new Place("Nile at Luxor", 25.70, 32.64),
    new Place("Amazon at Manaus", -3.14, -59.98),
    new Place("Volga at Volgograd", 48.70, 44.52),
    new Place("Mississippi at Memphis", 35.13, -90.07),
    new Place("Yangtze at Wuhan", 30.58, 114.28),
    new Place("Danube at Budapest", 47.50, 19.05),
    new Place("Congo at Kisangani", 0.50, 25.20),
    new Place("Ob at Novosibirsk", 55.00, 82.90),
    new Place("Ganges at Varanasi", 25.30, 83.00),
    new Place("Murray at Mildura", -34.18, 142.16),
    new Place("Mississippi delta", 29.15, -89.25),
    new Place("Niger delta", 4.40, 6.10),
  };

  private static final Place[] DRY = {
    new Place("Central Sahara", 23.0, 10.0),
    new Place("Rub' al Khali", 20.0, 50.0),
    new Place("Gobi", 42.5, 105.0),
    new Place("Greenland ice sheet", 72.0, -40.0),
  };

  @Test
  public void realRiversStandOnTheMap() {
    final RegionGenerator generator = new RegionGenerator(
      BuiltinWorldPreset.defaultSettings(),
      Seed.of(SEED)
    );
    final List<String> failures = new ArrayList<>();
    final StringBuilder report = new StringBuilder("\n=== Map rivers ===\n");
    for (final Place place : ON_RIVERS) {
      final double distance = distanceToRiver(generator, place);
      report.append(line(place, distance));
      if (distance > RIVER_TOLERANCE_CELLS) {
        failures.add("no river at " + place.name());
      }
    }
    for (final Place place : DRY) {
      final double distance = distanceToRiver(generator, place);
      report.append(line(place, distance));
      if (distance < DRY_DISTANCE_CELLS) {
        failures.add("river at " + place.name());
      }
    }
    // Past the east edge of the map the world is mirrored, rivers included.
    final int[] manaus = WaypointCoordinates.toBlockXZ(-3.14, -59.98);
    final double mirrored = distanceToRiver(
      generator,
      Units.blockToGridExact(
        2 * TFCRealWorldConfig.HORIZONTAL_SCALE.get() - manaus[0]
      ),
      Units.blockToGridExact(manaus[1])
    );
    report.append(
      String.format(Locale.ROOT, "Mirrored Amazon: %.2f cells%n", mirrored)
    );
    if (mirrored > RIVER_TOLERANCE_CELLS) {
      failures.add("no river on the mirrored map");
    }
    System.out.println(report);
    assertEquals(List.of(), failures);
  }

  private static String line(Place place, double distance) {
    return String.format(
      Locale.ROOT,
      "%-24s nearest river %s cells%n",
      place.name(),
      distance == Double.MAX_VALUE
        ? "none"
        : String.format(Locale.ROOT, "%.2f", distance)
    );
  }

  /** Distance in grid cells to the nearest river edge around the place. */
  private static double distanceToRiver(
    RegionGenerator generator,
    Place place
  ) {
    final int[] block = WaypointCoordinates.toBlockXZ(
      place.latitude(),
      place.longitude()
    );
    return distanceToRiver(
      generator,
      Units.blockToGridExact(block[0]),
      Units.blockToGridExact(block[1])
    );
  }

  private static double distanceToRiver(
    RegionGenerator generator,
    double gridX,
    double gridZ
  ) {
    double nearest = Double.MAX_VALUE;
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        for (final RiverEdge edge : generator
          .getOrCreatePartitionPoint(
            (int) Math.floor(gridX) + dx * Units.PARTITION_WIDTH_IN_GRID,
            (int) Math.floor(gridZ) + dz * Units.PARTITION_WIDTH_IN_GRID
          )
          .rivers()) {
          nearest = Math.min(
            nearest,
            Math.sqrt(edge.fractal().intersectDistance(gridX, gridZ))
          );
        }
      }
    }
    return nearest;
  }
}
